package com.ferramentafacil.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.activity.AvaliarActivity;
import com.ferramentafacil.app.activity.ChatActivity;
import com.ferramentafacil.app.activity.DetalhesFerramentaActivity;
import com.ferramentafacil.app.adapter.RentalAdapter;
import com.ferramentafacil.app.model.Rental;
import com.ferramentafacil.app.model.RentalStatus;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.ListaListener;
import com.ferramentafacil.app.repository.RentalRepository;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Aluguéis do usuário como LOCATÁRIO, separados em abas:
 * Aguardando aprovação • Aprovados • Em andamento • Concluídos • Cancelados.
 * Atualiza em tempo real quando o proprietário responde.
 */
public class MeusAlugueisFragment extends Fragment implements RentalAdapter.Acoes {

    private TabLayout tabs;
    private RentalAdapter adapter;
    private ProgressBar progress;
    private TextView tvVazio;
    private ListenerRegistration registro;
    private int abaPendente = -1;

    private final List<Rental> todos = new ArrayList<>();
    private final RentalRepository rentalRepo = new RentalRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        return inflater.inflate(R.layout.fragment_meus_alugueis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        progress = view.findViewById(R.id.progress);
        tvVazio = view.findViewById(R.id.tvVazio);
        tabs = view.findViewById(R.id.tabs);
        for (String aba : RentalStatus.ABAS_LOCATARIO) tabs.addTab(tabs.newTab().setText(aba));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filtrar(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        RecyclerView rv = view.findViewById(R.id.rvLista);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RentalAdapter(RentalAdapter.MODO_LOCATARIO, this);
        rv.setAdapter(adapter);

        if (abaPendente >= 0) {
            selecionarAba(abaPendente);
            abaPendente = -1;
        }
    }

    /** Chamado pela MainActivity para abrir uma aba específica. */
    public void selecionarAba(int indice) {
        if (tabs == null) {
            abaPendente = indice;
            return;
        }
        TabLayout.Tab t = tabs.getTabAt(indice);
        if (t != null) t.select();
    }

    @Override
    public void onStart() {
        super.onStart();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        progress.setVisibility(View.VISIBLE);
        registro = rentalRepo.escutarComoLocatario(uid, new ListaListener<Rental>() {
            @Override
            public void onLista(List<Rental> itens) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                todos.clear();
                todos.addAll(itens);
                filtrar();
            }

            @Override
            public void onErro(String mensagem) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                tvVazio.setVisibility(View.VISIBLE);
                tvVazio.setText(mensagem);
            }
        });
    }

    @Override
    public void onStop() {
        if (registro != null) registro.remove();
        super.onStop();
    }

    private void filtrar() {
        int aba = tabs.getSelectedTabPosition();
        List<Rental> lista = new ArrayList<>();
        for (Rental r : todos) if (RentalStatus.abaLocatario(r.status) == aba) lista.add(r);
        adapter.setItens(lista);
        tvVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        tvVazio.setText("Nenhum aluguel nesta aba.\nEncontre uma ferramenta na tela inicial!");
    }

    @Override
    public void onCancelar(Rental r) {
        UiUtils.confirmar(requireContext(), "Cancelar solicitação?",
                "A solicitação de \"" + r.toolNome + "\" será cancelada e o proprietário avisado.",
                "Cancelar solicitação", () -> rentalRepo.cancelarPeloLocatario(r, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void x) {
                        UiUtils.toast(getContext(), "Solicitação cancelada.");
                    }

                    @Override
                    public void onError(String m) {
                        UiUtils.toast(getContext(), m);
                    }
                }));
    }

    @Override
    public void onAvaliar(Rental r) {
        AvaliarActivity.abrir(requireContext(), r.rentalId);
    }

    @Override
    public void onChat(Rental r) {
        ChatActivity.abrirDeAluguel(requireContext(), r);
    }

    @Override
    public void onAbrirFerramenta(Rental r) {
        DetalhesFerramentaActivity.abrir(requireContext(), r.toolId);
    }
}
