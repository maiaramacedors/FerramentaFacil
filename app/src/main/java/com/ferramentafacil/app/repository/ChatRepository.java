package com.ferramentafacil.app.repository;

import androidx.annotation.Nullable;

import com.ferramentafacil.app.model.Chat;
import com.ferramentafacil.app.model.Message;
import com.ferramentafacil.app.model.Notificacao;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.FirebaseErrorUtils;
import com.ferramentafacil.app.utils.TextoUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat entre proprietário e interessado.
 * Estrutura: chats/{toolId_renterId} (cabeçalho) e chats/{id}/messages/{msgId} (mensagens).
 * Assim cada conversa fica associada à ferramenta e, quando houver, à solicitação de aluguel.
 */
public class ChatRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static String chatId(String toolId, String renterId) {
        return toolId + "_" + renterId;
    }

    /** Dados do cabeçalho da conversa. participantes = [proprietário, locatário]. */
    public static Map<String, Object> montarMeta(String toolId, String toolNome, String toolImagem,
                                                 String ownerId, String ownerNome,
                                                 String renterId, String renterNome,
                                                 @Nullable String rentalId) {
        Map<String, Object> m = new HashMap<>();
        m.put("toolId", toolId);
        m.put("toolNome", TextoUtils.seguro(toolNome));
        m.put("toolImagem", TextoUtils.seguro(toolImagem));
        m.put("participantes", Arrays.asList(ownerId, renterId));
        Map<String, Object> nomes = new HashMap<>();
        nomes.put(ownerId, TextoUtils.seguro(ownerNome));
        nomes.put(renterId, TextoUtils.seguro(renterNome));
        m.put("nomes", nomes);
        if (rentalId != null) m.put("rentalId", rentalId);
        m.put("atualizadoEm", System.currentTimeMillis());
        return m;
    }

    private DocumentReference chatRef(String chatId) {
        return db.collection(Constants.COL_CHATS).document(chatId);
    }

    /** Cria (ou atualiza) o cabeçalho da conversa antes de ouvir as mensagens. */
    public void garantirChat(String chatId, Map<String, Object> meta, Callback<Void> cb) {
        chatRef(chatId).set(meta, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public ListenerRegistration escutarMensagens(String chatId, EventListener<QuerySnapshot> l) {
        return chatRef(chatId).collection(Constants.COL_MESSAGES)
                .orderBy("dataHora", Query.Direction.ASCENDING)
                .addSnapshotListener(l);
    }

    /** Envia a mensagem, atualiza o cabeçalho e notifica o destinatário — tudo de uma vez. */
    public void enviar(String chatId, Map<String, Object> meta, String texto, String remetenteId,
                       String remetenteNome, String destinatarioId, Callback<Void> cb) {
        long agora = System.currentTimeMillis();
        Map<String, Object> cabecalho = new HashMap<>(meta);
        cabecalho.put("ultimaMensagem", texto);
        cabecalho.put("atualizadoEm", agora);

        Message msg = new Message();
        msg.chatId = chatId;
        msg.rentalId = (String) meta.get("rentalId");
        msg.toolId = (String) meta.get("toolId");
        msg.senderId = remetenteId;
        msg.senderNome = remetenteNome;
        msg.receiverId = destinatarioId;
        msg.mensagem = texto;
        msg.dataHora = agora;

        String resumo = texto.length() > 80 ? texto.substring(0, 80) + "…" : texto;

        WriteBatch b = db.batch();
        b.set(chatRef(chatId), cabecalho, SetOptions.merge());
        b.set(chatRef(chatId).collection(Constants.COL_MESSAGES).document(), msg);
        b.set(new NotificationRepository().novaRef(), NotificationRepository.montar(destinatarioId,
                "Nova mensagem de " + remetenteNome, resumo, Notificacao.TIPO_MENSAGEM, chatId));
        b.commit()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    /** Conversas do usuário, da mais recente para a mais antiga. */
    public void listarConversas(String uid, Callback<List<Chat>> cb) {
        db.collection(Constants.COL_CHATS).whereArrayContains("participantes", uid).get()
                .addOnSuccessListener(snap -> {
                    List<Chat> lista = snap.toObjects(Chat.class);
                    Collections.sort(lista, (a, b) -> Long.compare(b.atualizadoEm, a.atualizadoEm));
                    cb.onSuccess(lista);
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }
}
