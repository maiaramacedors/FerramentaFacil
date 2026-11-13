package com.ferramentafacil.app.repository;

import com.ferramentafacil.app.model.Notificacao;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.FirebaseErrorUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notificações internas (coleção "notifications").
 * Cada ação importante (nova solicitação, aprovação, recusa, mensagem, avaliação)
 * gera um documento para o destinatário.
 */
public class NotificationRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Monta o documento de notificação (o remetente é sempre o usuário logado). */
    public static Map<String, Object> montar(String destinatarioId, String titulo, String mensagem,
                                             String tipo, String refId) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", destinatarioId);
        m.put("remetenteId", FirebaseAuth.getInstance().getUid());
        m.put("titulo", titulo);
        m.put("mensagem", mensagem);
        m.put("tipo", tipo);
        m.put("refId", refId);
        m.put("lida", false);
        m.put("data", System.currentTimeMillis());
        return m;
    }

    public DocumentReference novaRef() {
        return db.collection(Constants.COL_NOTIFICATIONS).document();
    }

    /** Envia uma notificação (sem aguardar resultado). */
    public void enviar(String destinatarioId, String titulo, String mensagem, String tipo, String refId) {
        if (destinatarioId == null) return;
        db.collection(Constants.COL_NOTIFICATIONS).add(montar(destinatarioId, titulo, mensagem, tipo, refId));
    }

    public ListenerRegistration escutarNaoLidas(String uid, EventListener<QuerySnapshot> listener) {
        return db.collection(Constants.COL_NOTIFICATIONS)
                .whereEqualTo("userId", uid)
                .whereEqualTo("lida", false)
                .addSnapshotListener(listener);
    }

    public void listar(String uid, Callback<List<Notificacao>> cb) {
        db.collection(Constants.COL_NOTIFICATIONS).whereEqualTo("userId", uid).get()
                .addOnSuccessListener(snap -> {
                    List<Notificacao> lista = snap.toObjects(Notificacao.class);
                    Collections.sort(lista, (a, b) -> Long.compare(b.data, a.data));
                    cb.onSuccess(lista);
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public void marcarComoLidas(List<Notificacao> lista) {
        WriteBatch b = db.batch();
        int n = 0;
        for (Notificacao x : lista) {
            if (!x.lida && x.id != null && n < 450) {
                b.update(db.collection(Constants.COL_NOTIFICATIONS).document(x.id), "lida", true);
                n++;
            }
        }
        if (n > 0) b.commit();
    }
}
