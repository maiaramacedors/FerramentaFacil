package com.ferramentafacil.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.adapter.NotificacaoAdapter;
import com.ferramentafacil.app.model.Notificacao;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.NotificationRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/** Lista de notificações; ao abrir, marca todas como lidas. */
public class NotificacoesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Notificações");
        toolbar.setNavigationOnClickListener(v -> finish());

        ProgressBar progress = findViewById(R.id.progress);
        TextView tvVazio = findViewById(R.id.tvVazio);
        RecyclerView rv = findViewById(R.id.rvLista);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        NotificacaoAdapter adapter = new NotificacaoAdapter(this::abrir);
        rv.setAdapter(adapter);

        NotificationRepository repo = new NotificationRepository();
        repo.listar(uid, new Callback<List<Notificacao>>() {
            @Override
            public void onSuccess(List<Notificacao> lista) {
                progress.setVisibility(View.GONE);
                adapter.setItens(lista);
                tvVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
                tvVazio.setText("Você não tem notificações.");
                repo.marcarComoLidas(lista);
            }

            @Override
            public void onError(String m) {
                progress.setVisibility(View.GONE);
                tvVazio.setVisibility(View.VISIBLE);
                tvVazio.setText(m);
            }
        });
    }

    /** Leva o usuário para a tela relacionada à notificação. */
    private void abrir(Notificacao n) {
        if (n.tipo == null) return;
        switch (n.tipo) {
            case Notificacao.TIPO_SOLICITACAO:
                startActivity(new Intent(this, SolicitacoesActivity.class));
                break;
            case Notificacao.TIPO_STATUS:
                startActivity(MainActivity.intentAba(this, MainActivity.ABA_ALUGUEIS, -1));
                break;
            case Notificacao.TIPO_MENSAGEM:
                startActivity(new Intent(this, ConversasActivity.class));
                break;
            case Notificacao.TIPO_AVALIACAO:
                AvaliacoesActivity.abrir(this, FirebaseAuth.getInstance().getUid());
                break;
            default:
                break;
        }
    }
}
