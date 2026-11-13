package com.ferramentafacil.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.adapter.MessageAdapter;
import com.ferramentafacil.app.model.Chat;
import com.ferramentafacil.app.model.Message;
import com.ferramentafacil.app.model.Rental;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.ChatRepository;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Map;

/**
 * Chat em tempo real entre proprietário e interessado.
 * A conversa é ligada à ferramenta (e à solicitação de aluguel, quando existir).
 */
public class ChatActivity extends AppCompatActivity {

    private static final String E_CHAT = "chatId", E_TOOL = "toolId", E_TOOL_NOME = "toolNome",
            E_TOOL_IMG = "toolImagem", E_OWNER = "ownerId", E_OWNER_NOME = "ownerNome",
            E_RENTER = "renterId", E_RENTER_NOME = "renterNome", E_RENTAL = "rentalId";

    public static void abrir(Context c, String chatId, String toolId, String toolNome, String toolImagem,
                             String ownerId, String ownerNome, String renterId, String renterNome,
                             @Nullable String rentalId) {
        c.startActivity(new Intent(c, ChatActivity.class)
                .putExtra(E_CHAT, chatId).putExtra(E_TOOL, toolId).putExtra(E_TOOL_NOME, toolNome)
                .putExtra(E_TOOL_IMG, toolImagem).putExtra(E_OWNER, ownerId).putExtra(E_OWNER_NOME, ownerNome)
                .putExtra(E_RENTER, renterId).putExtra(E_RENTER_NOME, renterNome).putExtra(E_RENTAL, rentalId));
    }

    public static void abrirDeAluguel(Context c, Rental r) {
        abrir(c, ChatRepository.chatId(r.toolId, r.renterId), r.toolId, r.toolNome, r.toolImagem,
                r.ownerId, r.ownerNome, r.renterId, r.renterNome, r.rentalId);
    }

    public static void abrirDeConversa(Context c, Chat chat) {
        String ownerId = chat.participantes.get(0);
        String renterId = chat.participantes.get(1);
        String ownerNome = chat.nomes != null ? chat.nomes.get(ownerId) : "";
        String renterNome = chat.nomes != null ? chat.nomes.get(renterId) : "";
        abrir(c, chat.chatId, chat.toolId, chat.toolNome, chat.toolImagem, ownerId, ownerNome,
                renterId, renterNome, chat.rentalId);
    }

    private RecyclerView rv;
    private EditText etMensagem;
    private TextView tvVazio;
    private ProgressBar progress;
    private MessageAdapter adapter;
    private ListenerRegistration registro;

    private String chatId, uid, outroId, meuNome;
    private Map<String, Object> meta;
    private final ChatRepository chatRepo = new ChatRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent i = getIntent();
        chatId = i.getStringExtra(E_CHAT);
        String toolId = i.getStringExtra(E_TOOL);
        String toolNome = i.getStringExtra(E_TOOL_NOME);
        String ownerId = i.getStringExtra(E_OWNER);
        String ownerNome = i.getStringExtra(E_OWNER_NOME);
        String renterId = i.getStringExtra(E_RENTER);
        String renterNome = i.getStringExtra(E_RENTER_NOME);
        uid = FirebaseAuth.getInstance().getUid();
        if (chatId == null || uid == null || ownerId == null || renterId == null) {
            finish();
            return;
        }
        boolean souDono = uid.equals(ownerId);
        outroId = souDono ? renterId : ownerId;
        meuNome = souDono ? ownerNome : renterNome;
        String outroNome = souDono ? renterNome : ownerNome;

        meta = ChatRepository.montarMeta(toolId, toolNome, i.getStringExtra(E_TOOL_IMG), ownerId, ownerNome,
                renterId, renterNome, i.getStringExtra(E_RENTAL));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(outroNome != null && !outroNome.isEmpty() ? outroNome : "Conversa");
        toolbar.setSubtitle("Sobre: " + toolNome);
        toolbar.setNavigationOnClickListener(v -> finish());

        rv = findViewById(R.id.rvMensagens);
        etMensagem = findViewById(R.id.etMensagem);
        tvVazio = findViewById(R.id.tvVazio);
        progress = findViewById(R.id.progress);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        adapter = new MessageAdapter(uid);
        rv.setAdapter(adapter);

        findViewById(R.id.btnEnviar).setOnClickListener(v -> enviar());

        // Garante o cabeçalho da conversa e então passa a ouvir as mensagens
        chatRepo.garantirChat(chatId, meta, new Callback<Void>() {
            @Override
            public void onSuccess(Void r) {
                escutar();
            }

            @Override
            public void onError(String mensagem) {
                progress.setVisibility(View.GONE);
                UiUtils.toast(ChatActivity.this, mensagem);
            }
        });
    }

    private void escutar() {
        registro = chatRepo.escutarMensagens(chatId, (snap, e) -> {
            progress.setVisibility(View.GONE);
            if (e != null || snap == null) {
                UiUtils.toast(this, "Não foi possível carregar as mensagens.");
                return;
            }
            List<Message> lista = snap.toObjects(Message.class);
            adapter.setItens(lista);
            tvVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            if (!lista.isEmpty()) rv.scrollToPosition(lista.size() - 1);
        });
    }

    private void enviar() {
        String texto = UiUtils.texto(etMensagem);
        if (texto.isEmpty()) return;
        etMensagem.setText("");
        chatRepo.enviar(chatId, meta, texto, uid, meuNome != null ? meuNome : "", outroId, new Callback<Void>() {
            @Override public void onSuccess(Void r) { }

            @Override
            public void onError(String mensagem) {
                UiUtils.toast(ChatActivity.this, "Mensagem não enviada: " + mensagem);
                etMensagem.setText(texto);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (registro != null) registro.remove();
        super.onDestroy();
    }
}
