package com.ferramentafacil.app.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.adapter.RentalAdapter;
import com.ferramentafacil.app.model.Rental;
import com.ferramentafacil.app.model.RentalStatus;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.ListaListener;
import com.ferramentafacil.app.repository.RentalRepository;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.DateUtils;
import com.ferramentafacil.app.utils.MoneyUtils;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Solicitações recebidas pelo proprietário (em tempo real), com
 * ACEITAR / RECUSAR, confirmar retirada e devolução, cancelar e chat.
 */
public class SolicitacoesActivity extends AppCompatActivity implements RentalAdapter.Acoes {

    public static final String EXTRA_TITULO = "extra_titulo";

    private TabLayout tabs;
    private RentalAdapter adapter;
    private ProgressBar progress;
    private TextView tvVazio;
    private ListenerRegistration registro;
    private String toolId;

    private final List<Rental> todas = new ArrayList<>();
    private final RentalRepository rentalRepo = new RentalRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitacoes);

        toolId = getIntent().getStringExtra(Constants.EXTRA_TOOL_ID);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        String titulo = getIntent().getStringExtra(EXTRA_TITULO);
        if (titulo != null) toolbar.setSubtitle(titulo);

        progress = findViewById(R.id.progress);
        tvVazio = findViewById(R.id.tvVazio);
        tabs = findViewById(R.id.tabs);
        for (String aba : RentalStatus.ABAS_PROPRIETARIO) tabs.addTab(tabs.newTab().setText(aba));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filtrar(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        RecyclerView rv = findViewById(R.id.rvLista);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RentalAdapter(RentalAdapter.MODO_PROPRIETARIO, this);
        rv.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        progress.setVisibility(View.VISIBLE);
        registro = rentalRepo.escutarComoProprietario(uid, toolId, new ListaListener<Rental>() {
            @Override
            public void onLista(List<Rental> itens) {
                progress.setVisibility(View.GONE);
                todas.clear();
                todas.addAll(itens);
                atualizarTitulosAbas();
                filtrar();
            }

            @Override
            public void onErro(String mensagem) {
                progress.setVisibility(View.GONE);
                tvVazio.setVisibility(View.VISIBLE);
                tvVazio.setText(mensagem);
            }
        });
    }

    @Override
    protected void onStop() {
        if (registro != null) registro.remove();
        super.onStop();
    }

    private void atualizarTitulosAbas() {
        int pendentes = 0;
        for (Rental r : todas) if (RentalStatus.PENDENTE.equals(r.status)) pendentes++;
        TabLayout.Tab aba = tabs.getTabAt(0);
        if (aba != null) aba.setText(pendentes > 0 ? "Pendentes (" + pendentes + ")" : "Pendentes");
    }

    private void filtrar() {
        int aba = tabs.getSelectedTabPosition();
        List<Rental> lista = new ArrayList<>();
        for (Rental r : todas) if (RentalStatus.abaProprietario(r.status) == aba) lista.add(r);
        adapter.setItens(lista);
        tvVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        tvVazio.setText("Nenhuma solicitação nesta aba.");
    }

    private Callback<Void> resultado(String sucesso) {
        AlertDialog loading = UiUtils.carregando(this, "Atualizando...");
        loading.show();
        return new Callback<Void>() {
            @Override
            public void onSuccess(Void r) {
                loading.dismiss();
                UiUtils.toast(SolicitacoesActivity.this, sucesso);
            }

            @Override
            public void onError(String mensagem) {
                loading.dismiss();
                UiUtils.alerta(SolicitacoesActivity.this, "Atenção", mensagem);
            }
        };
    }

    private String resumo(Rental r) {
        return r.renterNome + " · " + DateUtils.formatarData(r.dataRetirada) + " a "
                + DateUtils.formatarData(r.dataDevolucao) + " (" + r.quantidadeDias + " dias) · "
                + MoneyUtils.formatar(r.valorTotal);
    }

    @Override
    public void onAceitar(Rental r) {
        UiUtils.confirmar(this, "Aceitar solicitação?",
                resumo(r) + "\n\nO período ficará reservado e o interessado verá o endereço de retirada.",
                "Aceitar", () -> rentalRepo.aprovar(r, resultado("Solicitação aprovada!")));
    }

    @Override
    public void onRecusar(Rental r) {
        UiUtils.confirmar(this, "Recusar solicitação?", resumo(r), "Recusar",
                () -> rentalRepo.recusar(r, resultado("Solicitação recusada.")));
    }

    @Override
    public void onIniciar(Rental r) {
        UiUtils.confirmar(this, "Confirmar retirada?",
                "Confirme que " + r.renterNome + " retirou a ferramenta.", "Confirmar",
                () -> rentalRepo.iniciar(r, resultado("Aluguel em andamento.")));
    }

    @Override
    public void onConcluir(Rental r) {
        UiUtils.confirmar(this, "Confirmar devolução?",
                "Confirme que a ferramenta foi devolvida. O aluguel será concluído e o locatário poderá avaliar.",
                "Confirmar", () -> rentalRepo.concluir(r, resultado("Aluguel concluído!")));
    }

    @Override
    public void onCancelar(Rental r) {
        UiUtils.confirmar(this, "Cancelar aluguel aprovado?",
                "O período será liberado e o locatário será avisado.", "Cancelar aluguel",
                () -> rentalRepo.cancelarPeloProprietario(r, resultado("Aluguel cancelado.")));
    }

    @Override
    public void onChat(Rental r) {
        ChatActivity.abrirDeAluguel(this, r);
    }

    @Override
    public void onAbrirFerramenta(Rental r) {
        DetalhesFerramentaActivity.abrir(this, r.toolId);
    }
}
