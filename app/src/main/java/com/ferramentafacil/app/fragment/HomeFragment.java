package com.ferramentafacil.app.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.activity.BuscaActivity;
import com.ferramentafacil.app.activity.DetalhesFerramentaActivity;
import com.ferramentafacil.app.activity.NotificacoesActivity;
import com.ferramentafacil.app.adapter.CategoryAdapter;
import com.ferramentafacil.app.adapter.ToolAdapter;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.ToolRepository;
import com.ferramentafacil.app.repository.UserRepository;
import com.ferramentafacil.app.service.LocationHelper;
import com.ferramentafacil.app.service.NotificacaoWatcher;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.LocationUtils;
import com.ferramentafacil.app.utils.PrefsManager;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tela inicial: saudação, localização, busca, categorias, destaques e ferramentas próximas.
 */
public class HomeFragment extends Fragment {

    private TextView tvSaudacao, tvLocalizacao, tvBadge, tvVazio, tvRaio, tvTituloProximos;
    private ProgressBar progress;
    private SwipeRefreshLayout swipe;
    private ToolAdapter destaquesAdapter, proximosAdapter;
    private PrefsManager prefs;
    private String uid;

    private final ToolRepository toolRepo = new ToolRepository();
    private final UserRepository userRepo = new UserRepository();
    private final NotificacaoWatcher.Listener badgeListener = this::atualizarBadge;

    /** Pedido de permissão de localização (Android 6+). */
    private final ActivityResultLauncher<String[]> permissaoLocal = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                if (!isAdded()) return;
                if (LocationHelper.temPermissao(requireContext())) {
                    obterLocalizacaoAtual();
                } else {
                    tvLocalizacao.setText("Localização não definida");
                    UiUtils.toast(getContext(), "Sem permissão de localização. Informe sua cidade manualmente.");
                    dialogAlterarLocal();
                    carregar();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        prefs = new PrefsManager(requireContext());
        uid = FirebaseAuth.getInstance().getUid();

        tvSaudacao = view.findViewById(R.id.tvSaudacao);
        tvLocalizacao = view.findViewById(R.id.tvLocalizacao);
        tvBadge = view.findViewById(R.id.tvBadge);
        tvVazio = view.findViewById(R.id.tvVazio);
        tvRaio = view.findViewById(R.id.tvRaio);
        tvTituloProximos = view.findViewById(R.id.tvTituloProximos);
        progress = view.findViewById(R.id.progress);
        swipe = view.findViewById(R.id.swipeRefresh);

        RecyclerView rvCategorias = view.findViewById(R.id.rvCategorias);
        rvCategorias.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategorias.setAdapter(new CategoryAdapter(this::abrirBuscaCategoria));

        destaquesAdapter = new ToolAdapter(true, this::abrirDetalhes);
        RecyclerView rvDestaques = view.findViewById(R.id.rvDestaques);
        rvDestaques.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvDestaques.setAdapter(destaquesAdapter);

        proximosAdapter = new ToolAdapter(false, this::abrirDetalhes);
        RecyclerView rvProximos = view.findViewById(R.id.rvProximos);
        rvProximos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvProximos.setAdapter(proximosAdapter);

        view.findViewById(R.id.searchBar).setOnClickListener(v ->
                startActivity(new Intent(getContext(), BuscaActivity.class)));
        view.findViewById(R.id.btnNotificacoes).setOnClickListener(v ->
                startActivity(new Intent(getContext(), NotificacoesActivity.class)));
        view.findViewById(R.id.btnAlterarLocal).setOnClickListener(v -> dialogAlterarLocal());

        swipe.setColorSchemeResources(R.color.laranja);
        swipe.setOnRefreshListener(() -> {
            UserRepository.limparCache();
            carregar();
        });

        carregarSaudacao();
        if (prefs.temLocalizacao()) {
            mostrarLocal();
            carregar();
        } else {
            solicitarLocalizacao();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        NotificacaoWatcher.get().adicionar(badgeListener);
    }

    @Override
    public void onStop() {
        NotificacaoWatcher.get().remover(badgeListener);
        super.onStop();
    }

    private void atualizarBadge(int total) {
        if (tvBadge == null) return;
        tvBadge.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        tvBadge.setText(total > 9 ? "9+" : String.valueOf(total));
    }

    private void carregarSaudacao() {
        if (uid == null) return;
        userRepo.buscar(uid, new Callback<User>() {
            @Override
            public void onSuccess(User u) {
                if (isAdded()) tvSaudacao.setText("Olá, " + u.primeiroNome() + "!");
            }

            @Override
            public void onError(String m) { }
        });
    }

    // ------------------------------------------------------------ Localização

    private void solicitarLocalizacao() {
        if (LocationHelper.temPermissao(requireContext())) obterLocalizacaoAtual();
        else permissaoLocal.launch(LocationHelper.PERMISSOES);
    }

    private void obterLocalizacaoAtual() {
        tvLocalizacao.setText("Obtendo sua localização...");
        Context app = requireContext().getApplicationContext();
        LocationHelper.obterLocalizacaoAtual(app, new LocationHelper.ResultadoLocalizacao() {
            @Override
            public void onLocalizacao(double lat, double lng) {
                LocationHelper.enderecoDe(app, lat, lng, endereco -> {
                    prefs.salvarLocalizacao(lat, lng, LocationHelper.descreverCidade(endereco));
                    if (!isAdded()) return;
                    mostrarLocal();
                    carregar();
                });
            }

            @Override
            public void onErro(String mensagem) {
                if (!isAdded()) return;
                tvLocalizacao.setText(prefs.temLocalizacao() ? prefs.getCidade() : "Localização não definida");
                UiUtils.toast(getContext(), mensagem);
                carregar();
            }
        });
    }

    private void mostrarLocal() {
        tvLocalizacao.setText(prefs.getCidade());
        tvRaio.setText("até " + Math.round(prefs.getRaioKm()) + " km");
    }

    /** Permite usar o GPS ou digitar uma cidade, e ajustar o raio de busca. */
    private void dialogAlterarLocal() {
        View v = getLayoutInflater().inflate(R.layout.dialog_alterar_localizacao, null);
        TextInputLayout tilCidade = v.findViewById(R.id.tilCidade);
        TextInputEditText etCidade = v.findViewById(R.id.etCidade);
        TextView tvRaioDialog = v.findViewById(R.id.tvRaio);
        Slider slider = v.findViewById(R.id.sliderRaio);

        float raio = Math.max(5, Math.min(100, Math.round(prefs.getRaioKm() / 5f) * 5f));
        slider.setValue(raio);
        tvRaioDialog.setText("Raio de busca: " + Math.round(raio) + " km");
        slider.addOnChangeListener((s, valor, fromUser) ->
                tvRaioDialog.setText("Raio de busca: " + Math.round(valor) + " km"));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Onde você quer procurar?")
                .setView(v)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        v.findViewById(R.id.btnUsarGps).setOnClickListener(b -> {
            prefs.salvarRaioKm(slider.getValue());
            dialog.dismiss();
            solicitarLocalizacao();
        });

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            prefs.salvarRaioKm(slider.getValue());
            String cidade = UiUtils.texto(etCidade);
            if (cidade.isEmpty()) {
                dialog.dismiss();
                if (prefs.temLocalizacao()) mostrarLocal();
                carregar();
                return;
            }
            tilCidade.setError(null);
            tilCidade.setHelperText("Procurando...");
            LocationHelper.coordenadasDe(requireContext(), cidade, endereco -> {
                if (!isAdded()) return;
                if (endereco == null) {
                    tilCidade.setHelperText(null);
                    tilCidade.setError("Cidade não encontrada. Verifique o nome e a internet.");
                    return;
                }
                prefs.salvarLocalizacao(endereco.getLatitude(), endereco.getLongitude(),
                        LocationHelper.descreverCidade(endereco));
                dialog.dismiss();
                mostrarLocal();
                carregar();
            });
        }));
        dialog.show();
    }

    // ------------------------------------------------------------ Dados

    private void carregar() {
        if (!isAdded()) return;
        if (!swipe.isRefreshing() && proximosAdapter.getItemCount() == 0) progress.setVisibility(View.VISIBLE);
        tvVazio.setVisibility(View.GONE);

        toolRepo.listar(true, new Callback<List<Tool>>() {
            @Override
            public void onSuccess(List<Tool> lista) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                progress.setVisibility(View.GONE);
                exibir(lista);
            }

            @Override
            public void onError(String mensagem) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                progress.setVisibility(View.GONE);
                tvVazio.setVisibility(View.VISIBLE);
                tvVazio.setText(mensagem + "\nPuxe para baixo para tentar de novo.");
            }
        });
    }

    private void exibir(List<Tool> todas) {
        // Não mostra as ferramentas do próprio usuário
        List<Tool> outras = new ArrayList<>();
        for (Tool t : todas) if (t.ownerId != null && !t.ownerId.equals(uid)) outras.add(t);

        boolean temLocal = prefs.temLocalizacao();
        if (temLocal) {
            for (Tool t : outras) {
                t.distanciaKm = LocationUtils.distanciaKm(prefs.getLatitude(), prefs.getLongitude(), t.latitude, t.longitude);
            }
        }

        // Destaques: mais bem avaliadas (desempate: mais recentes)
        List<Tool> destaques = new ArrayList<>(outras);
        Collections.sort(destaques, (a, b) -> {
            int c = Double.compare(b.media(), a.media());
            return c != 0 ? c : Long.compare(b.dataCadastro, a.dataCadastro);
        });
        destaquesAdapter.setItens(destaques.subList(0, Math.min(10, destaques.size())));

        // Próximas: dentro do raio, ordenadas pela distância
        List<Tool> proximas = new ArrayList<>();
        if (temLocal) {
            float raio = prefs.getRaioKm();
            for (Tool t : outras) if (t.distanciaKm <= raio) proximas.add(t);
            Collections.sort(proximas, (a, b) -> Double.compare(a.distanciaKm, b.distanciaKm));
            tvTituloProximos.setText("Perto de você");
        } else {
            proximas.addAll(outras);
            Collections.sort(proximas, (a, b) -> Long.compare(b.dataCadastro, a.dataCadastro));
            tvTituloProximos.setText("Anúncios recentes");
        }
        proximosAdapter.setItens(proximas);

        if (outras.isEmpty()) {
            tvVazio.setVisibility(View.VISIBLE);
            tvVazio.setText("Ainda não há ferramentas anunciadas.\nQue tal ser o primeiro? Toque em \"Anunciar\".");
        } else if (proximas.isEmpty()) {
            tvVazio.setVisibility(View.VISIBLE);
            tvVazio.setText("Nenhuma ferramenta num raio de " + Math.round(prefs.getRaioKm())
                    + " km.\nToque em \"Alterar\" para aumentar o raio ou trocar a cidade.");
        } else {
            tvVazio.setVisibility(View.GONE);
        }
    }

    private void abrirDetalhes(Tool t) {
        DetalhesFerramentaActivity.abrir(requireContext(), t.toolId);
    }

    private void abrirBuscaCategoria(String categoria) {
        startActivity(new Intent(getContext(), BuscaActivity.class).putExtra(Constants.EXTRA_CATEGORIA, categoria));
    }
}
