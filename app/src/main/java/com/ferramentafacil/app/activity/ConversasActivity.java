package com.ferramentafacil.app.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.adapter.ConversaAdapter;
import com.ferramentafacil.app.model.Chat;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.ChatRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/** Lista de conversas do usuário (como proprietário e como interessado). */
public class ConversasActivity extends AppCompatActivity {

    private ConversaAdapter adapter;
    private ProgressBar progress;
    private TextView tvVazio;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        uid = FirebaseAuth.getInstance().getUid();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Minhas conversas");
        toolbar.setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.progress);
        tvVazio = findViewById(R.id.tvVazio);
        RecyclerView rv = findViewById(R.id.rvLista);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new ConversaAdapter(uid, chat -> ChatActivity.abrirDeConversa(this, chat));
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (uid == null) return;
        progress.setVisibility(View.VISIBLE);
        new ChatRepository().listarConversas(uid, new Callback<List<Chat>>() {
            @Override
            public void onSuccess(List<Chat> lista) {
                progress.setVisibility(View.GONE);
                adapter.setItens(lista);
                tvVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
                tvVazio.setText("Nenhuma conversa ainda.\nUse o botão \"Chat\" nos detalhes de uma ferramenta.");
            }

            @Override
            public void onError(String mensagem) {
                progress.setVisibility(View.GONE);
                tvVazio.setVisibility(View.VISIBLE);
                tvVazio.setText(mensagem);
            }
        });
    }
}
