package com.ferramentafacil.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.adapter.ImageSliderAdapter;
import com.ferramentafacil.app.adapter.ReviewAdapter;
import com.ferramentafacil.app.model.Review;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.ChatRepository;
import com.ferramentafacil.app.repository.FavoriteRepository;
import com.ferramentafacil.app.repository.RentalRepository;
import com.ferramentafacil.app.repository.ReviewRepository;
import com.ferramentafacil.app.repository.ToolRepository;
import com.ferramentafacil.app.repository.UserRepository;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.DateUtils;
import com.ferramentafacil.app.utils.LocationUtils;
import com.ferramentafacil.app.utils.MoneyUtils;
import com.ferramentafacil.app.utils.PeriodoOcupadoValidator;
import com.ferramentafacil.app.utils.PrefsManager;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Detalhes da ferramenta: fotos, informações, localização aproximada, proprietário,
 * calendário para escolher o período, cálculo automático do valor e botão ALUGAR AGORA.
 */
public class DetalhesFerramentaActivity extends AppCompatActivity {

    public static void abrir(Context c, String toolId) {
        c.startActivity(new Intent(c, DetalhesFerramentaActivity.class).putExtra(Constants.EXTRA_TOOL_ID, toolId));
    }

    private ViewPager2 vpImagens;
    private TextView tvContador, tvCategoria, tvNome, tvMarcaModelo, tvAvaliacaoFerramenta, tvEstado, tvPreco,
            tvLocalizacao, tvDistancia, tvDescricao, tvDonoNome, tvDonoAvaliacao, tvSemAvaliacoes,
            tvRetirada, tvDevolucao, tvDias, tvValorDiario, tvTotal, tvTituloPeriodo;
    private ImageView ivDono, ivVerificado;
    private ImageButton btnFavorito;
    private MaterialButton btnAlugar, btnSelecionarPeriodo, btnConversar;
    private MaterialCardView cardResumo;
    private ProgressBar progress;
    private ReviewAdapter reviewAdapter;

    private String toolId, uid;
    private Tool tool;
    private User eu;
    private Long retirada, devolucao;
    private boolean favorito = false;
    private boolean recarregarAoVoltar = false;

    private final ToolRepository toolRepo = new ToolRepository();
    private final UserRepository userRepo = new UserRepository();
    private final RentalRepository rentalRepo = new RentalRepository();
    private final FavoriteRepository favRepo = new FavoriteRepository();
    private final ReviewRepository reviewRepo = new ReviewRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_ferramenta);
        toolId = getIntent().getStringExtra(Constants.EXTRA_TOOL_ID);
        uid = FirebaseAuth.getInstance().getUid();
        if (toolId == null || uid == null) {
            finish();
            return;
        }
        ligarViews();
        carregar();
        userRepo.buscar(uid, new Callback<User>() {
            @Override public void onSuccess(User u) { eu = u; }
            @Override public void onError(String m) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recarregarAoVoltar) {
            recarregarAoVoltar = false;
            carregar();
        }
    }

    private void ligarViews() {
        vpImagens = findViewById(R.id.vpImagens);
        tvContador = findViewById(R.id.tvContadorImagens);
        tvCategoria = findViewById(R.id.tvCategoria);
        tvNome = findViewById(R.id.tvNome);
        tvMarcaModelo = findViewById(R.id.tvMarcaModelo);
        tvAvaliacaoFerramenta = findViewById(R.id.tvAvaliacaoFerramenta);
        tvEstado = findViewById(R.id.tvEstado);
        tvPreco = findViewById(R.id.tvPreco);
        tvLocalizacao = findViewById(R.id.tvLocalizacao);
        tvDistancia = findViewById(R.id.tvDistancia);
        tvDescricao = findViewById(R.id.tvDescricao);
        tvDonoNome = findViewById(R.id.tvDonoNome);
        tvDonoAvaliacao = findViewById(R.id.tvDonoAvaliacao);
        tvSemAvaliacoes = findViewById(R.id.tvSemAvaliacoes);
        tvRetirada = findViewById(R.id.tvRetirada);
        tvDevolucao = findViewById(R.id.tvDevolucao);
        tvDias = findViewById(R.id.tvDias);
        tvValorDiario = findViewById(R.id.tvValorDiario);
        tvTotal = findViewById(R.id.tvTotal);
        tvTituloPeriodo = findViewById(R.id.tvTituloPeriodo);
        ivDono = findViewById(R.id.ivDono);
        ivVerificado = findViewById(R.id.ivVerificado);
        btnFavorito = findViewById(R.id.btnFavorito);
        btnAlugar = findViewById(R.id.btnAlugar);
        btnSelecionarPeriodo = findViewById(R.id.btnSelecionarPeriodo);
        btnConversar = findViewById(R.id.btnConversar);
        cardResumo = findViewById(R.id.cardResumo);
        progress = findViewById(R.id.progress);

        RecyclerView rv = findViewById(R.id.rvAvaliacoes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(false);
        rv.setAdapter(reviewAdapter);

        vpImagens.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                atualizarContador(position);
            }
        });

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnMapa).setOnClickListener(v -> abrirMapa());
        btnFavorito.setOnClickListener(v -> alternarFavorito());
        btnSelecionarPeriodo.setOnClickListener(v -> abrirCalendario());
        btnConversar.setOnClickListener(v -> abrirChat());
        btnAlugar.setOnClickListener(v -> aoClicarAlugar());
        findViewById(R.id.cardProprietario).setOnClickListener(v -> {
            if (tool != null) AvaliacoesActivity.abrir(this, tool.ownerId);
        });
    }

    private void carregar() {
        progress.setVisibility(View.VISIBLE);
        toolRepo.buscarPorId(toolId, new Callback<Tool>() {
            @Override
            public void onSuccess(Tool t) {
                progress.setVisibility(View.GONE);
                tool = t;
                preencher();
                carregarAvaliacoes();
                verificarFavorito();
            }

            @Override
            public void onError(String mensagem) {
                progress.setVisibility(View.GONE);
                UiUtils.toast(DetalhesFerramentaActivity.this, mensagem);
                finish();
            }
        });
    }

    private void preencher() {
        vpImagens.setAdapter(new ImageSliderAdapter(tool.imagens));
        atualizarContador(0);

        tvCategoria.setText(tool.categoria);
        tvNome.setText(tool.nome);
        String marcaModelo = (UiTexto(tool.marca) + " " + UiTexto(tool.modelo)).trim();
        tvMarcaModelo.setVisibility(marcaModelo.isEmpty() ? View.GONE : View.VISIBLE);
        tvMarcaModelo.setText(marcaModelo);
        tvAvaliacaoFerramenta.setText(tool.totalAvaliacoes > 0
                ? String.format(new Locale("pt", "BR"), "%.1f (%d avaliações)", tool.media(), tool.totalAvaliacoes)
                : "Sem avaliações");
        tvEstado.setText("• " + tool.estadoConservacao);
        tvPreco.setText(MoneyUtils.formatar(tool.valorDiaria));
        tvDescricao.setText(tool.descricao);

        // Localização aproximada (nunca o endereço completo)
        tvLocalizacao.setText(tool.localizacaoAproximada() + " (aprox.)");
        PrefsManager prefs = new PrefsManager(this);
        if (prefs.temLocalizacao()) {
            double km = LocationUtils.distanciaKm(prefs.getLatitude(), prefs.getLongitude(), tool.latitude, tool.longitude);
            tvDistancia.setText("Cerca de " + LocationUtils.formatarDistancia(km) + " de você");
        } else {
            tvDistancia.setText("Defina sua localização na tela inicial para ver a distância");
        }

        // Proprietário
        UiUtils.carregarImagem(ivDono, tool.ownerFoto, R.drawable.placeholder_pessoa);
        tvDonoNome.setText(tool.ownerNome != null ? tool.ownerNome : "Proprietário");
        ivVerificado.setVisibility(tool.ownerVerificado ? View.VISIBLE : View.GONE);
        tvDonoAvaliacao.setText(tool.ownerTotalAvaliacoes > 0
                ? String.format(new Locale("pt", "BR"), "★ %.1f · %d avaliações · ver todas", tool.ownerNota, tool.ownerTotalAvaliacoes)
                : "Novo na plataforma");

        boolean meu = uid.equals(tool.ownerId);
        btnFavorito.setVisibility(meu ? View.GONE : View.VISIBLE);
        btnConversar.setVisibility(meu ? View.GONE : View.VISIBLE);
        btnSelecionarPeriodo.setVisibility(meu ? View.GONE : View.VISIBLE);
        tvTituloPeriodo.setVisibility(meu ? View.GONE : View.VISIBLE);
        if (meu) {
            btnAlugar.setText("EDITAR MEU ANÚNCIO");
            btnAlugar.setEnabled(true);
            cardResumo.setVisibility(View.GONE);
        } else if (!tool.disponivel) {
            btnAlugar.setText("INDISPONÍVEL NO MOMENTO");
            btnAlugar.setEnabled(false);
            btnSelecionarPeriodo.setEnabled(false);
        } else {
            btnAlugar.setText("ALUGAR AGORA");
            btnAlugar.setEnabled(true);
            btnSelecionarPeriodo.setEnabled(true);
            if (retirada != null) atualizarResumo();
        }
    }

    private static String UiTexto(String s) {
        return s == null ? "" : s;
    }

    private void atualizarContador(int pos) {
        int total = tool != null && tool.imagens != null ? Math.max(1, tool.imagens.size()) : 1;
        tvContador.setText((pos + 1) + "/" + total);
    }

    // ------------------------------------------------------------ Período e valor

    private void abrirCalendario() {
        if (tool == null) return;
        long hoje = DateUtils.hojeUtc();
        List<CalendarConstraints.DateValidator> validadores = new ArrayList<>();
        validadores.add(DateValidatorPointForward.from(hoje));
        validadores.add(new PeriodoOcupadoValidator(tool.periodosVigentes()));

        CalendarConstraints restricoes = new CalendarConstraints.Builder()
                .setStart(hoje)
                .setValidator(CompositeDateValidator.allOf(validadores))
                .build();

        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Escolha a retirada e a devolução")
                .setCalendarConstraints(restricoes);
        if (retirada != null && devolucao != null) builder.setSelection(new Pair<>(retirada, devolucao));

        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel.first == null || sel.second == null) return;
            String erro = DateUtils.validarPeriodo(sel.first, sel.second);
            if (erro == null && tool.temConflito(sel.first, sel.second, null)) {
                erro = "A ferramenta já está reservada em parte desse período. Os dias ocupados aparecem bloqueados no calendário.";
            }
            if (erro != null) {
                UiUtils.alerta(this, "Período indisponível", erro);
                return;
            }
            retirada = sel.first;
            devolucao = sel.second;
            atualizarResumo();
        });
        picker.show(getSupportFragmentManager(), "periodo");
    }

    /** Valor total = quantidade de dias × valor diário (calculado automaticamente). */
    private void atualizarResumo() {
        int dias = DateUtils.calcularDias(retirada, devolucao);
        double total = DateUtils.calcularTotal(tool.valorDiaria, dias);
        tvRetirada.setText(DateUtils.formatarData(retirada));
        tvDevolucao.setText(DateUtils.formatarData(devolucao));
        tvDias.setText(dias + (dias == 1 ? " dia" : " dias"));
        tvValorDiario.setText(MoneyUtils.formatar(tool.valorDiaria));
        tvTotal.setText(MoneyUtils.formatar(total));
        cardResumo.setVisibility(View.VISIBLE);
        btnSelecionarPeriodo.setText("Alterar período");
        btnAlugar.setText("ALUGAR AGORA · " + MoneyUtils.formatar(total));
    }

    private void aoClicarAlugar() {
        if (tool == null) return;
        if (uid.equals(tool.ownerId)) {
            recarregarAoVoltar = true;
            startActivity(new Intent(this, CadastrarFerramentaActivity.class)
                    .putExtra(Constants.EXTRA_TOOL_ID, tool.toolId));
            return;
        }
        if (retirada == null || devolucao == null) {
            UiUtils.toast(this, "Primeiro escolha as datas de retirada e devolução.");
            abrirCalendario();
            return;
        }
        if (eu == null) {
            UiUtils.toast(this, "Carregando seus dados, tente novamente em instantes.");
            return;
        }
        int dias = DateUtils.calcularDias(retirada, devolucao);
        double total = DateUtils.calcularTotal(tool.valorDiaria, dias);
        String resumo = "Ferramenta: " + tool.nome
                + "\nRetirada: " + DateUtils.formatarData(retirada)
                + "\nDevolução: " + DateUtils.formatarData(devolucao)
                + "\nQuantidade de dias: " + dias
                + "\nValor diário: " + MoneyUtils.formatar(tool.valorDiaria)
                + "\n\nVALOR TOTAL: " + MoneyUtils.formatar(total)
                + "\n\nO proprietário precisa aprovar a solicitação. O pagamento é combinado diretamente com ele.";
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar solicitação")
                .setMessage(resumo)
                .setNegativeButton("Voltar", null)
                .setPositiveButton("Solicitar aluguel", (d, w) -> solicitar())
                .show();
    }

    private void solicitar() {
        AlertDialog loading = UiUtils.carregando(this, "Enviando solicitação...");
        loading.show();
        rentalRepo.solicitar(tool, eu, retirada, devolucao, new Callback<String>() {
            @Override
            public void onSuccess(String rentalId) {
                loading.dismiss();
                new MaterialAlertDialogBuilder(DetalhesFerramentaActivity.this)
                        .setTitle("Solicitação enviada! 🎉")
                        .setMessage("Avisamos o proprietário. Você pode acompanhar o status em \"Meus aluguéis\" e conversar com ele pelo chat.")
                        .setCancelable(false)
                        .setPositiveButton("Ver meus aluguéis", (d, w) -> {
                            startActivity(MainActivity.intentAba(DetalhesFerramentaActivity.this, MainActivity.ABA_ALUGUEIS, 0));
                            finish();
                        })
                        .setNegativeButton("Continuar navegando", (d, w) -> finish())
                        .show();
            }

            @Override
            public void onError(String mensagem) {
                loading.dismiss();
                UiUtils.alerta(DetalhesFerramentaActivity.this, "Não foi possível solicitar", mensagem);
                carregar(); // atualiza disponibilidade e dias ocupados
            }
        });
    }

    // ------------------------------------------------------------ Outras ações

    private void abrirChat() {
        if (tool == null || eu == null) return;
        ChatActivity.abrir(this, ChatRepository.chatId(tool.toolId, uid), tool.toolId, tool.nome,
                tool.primeiraImagem(), tool.ownerId, tool.ownerNome, uid, eu.nome, null);
    }

    /** Abre o app de mapas na região APROXIMADA da ferramenta. */
    private void abrirMapa() {
        if (tool == null) return;
        String rotulo = Uri.encode("Região aproximada - " + tool.nome);
        Uri uri = Uri.parse("geo:" + tool.latitude + "," + tool.longitude
                + "?q=" + tool.latitude + "," + tool.longitude + "(" + rotulo + ")");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            UiUtils.toast(this, "Nenhum aplicativo de mapas encontrado.");
        }
    }

    private void verificarFavorito() {
        favRepo.verificar(toolId, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean r) {
                favorito = r;
                btnFavorito.setImageResource(favorito ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            }

            @Override
            public void onError(String m) { }
        });
    }

    private void alternarFavorito() {
        btnFavorito.setEnabled(false);
        favRepo.definir(toolId, !favorito, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean novo) {
                btnFavorito.setEnabled(true);
                favorito = novo;
                btnFavorito.setImageResource(favorito ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                UiUtils.toast(DetalhesFerramentaActivity.this, favorito ? "Adicionada aos favoritos" : "Removida dos favoritos");
            }

            @Override
            public void onError(String mensagem) {
                btnFavorito.setEnabled(true);
                UiUtils.toast(DetalhesFerramentaActivity.this, mensagem);
            }
        });
    }

    private void carregarAvaliacoes() {
        reviewRepo.listarDaFerramenta(toolId, new Callback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> lista) {
                tvSemAvaliacoes.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
                reviewAdapter.setItens(lista.subList(0, Math.min(5, lista.size())));
            }

            @Override
            public void onError(String m) {
                tvSemAvaliacoes.setVisibility(View.VISIBLE);
            }
        });
    }
}
