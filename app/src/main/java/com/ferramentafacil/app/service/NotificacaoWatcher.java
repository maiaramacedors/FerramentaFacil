package com.ferramentafacil.app.service;

import android.content.Context;

import com.ferramentafacil.app.model.Notificacao;
import com.ferramentafacil.app.repository.NotificationRepository;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Observa em tempo real as notificações não lidas do usuário logado:
 *  - atualiza o contador do sino na Home;
 *  - exibe uma notificação do Android quando chega algo novo (com o app aberto ou em segundo plano recente).
 *
 * Para notificações com o app FECHADO é necessário Firebase Cloud Messaging + Cloud Functions
 * (ver README, seção "Próximos passos").
 */
public final class NotificacaoWatcher {

    public interface Listener {
        void onNaoLidas(int total);
    }

    private static NotificacaoWatcher instancia;

    private final List<Listener> listeners = new ArrayList<>();
    private ListenerRegistration registro;
    private String uidAtual;
    private int total = 0;
    private long inicio;

    private NotificacaoWatcher() { }

    public static synchronized NotificacaoWatcher get() {
        if (instancia == null) instancia = new NotificacaoWatcher();
        return instancia;
    }

    public void iniciar(Context appCtx, String uid) {
        if (uid == null) return;
        if (uid.equals(uidAtual) && registro != null) return;
        parar();
        uidAtual = uid;
        inicio = System.currentTimeMillis();
        Context ctx = appCtx.getApplicationContext();
        registro = new NotificationRepository().escutarNaoLidas(uid, (snap, e) -> {
            if (e != null || snap == null) return;
            total = snap.size();
            avisar();
            for (DocumentChange dc : snap.getDocumentChanges()) {
                if (dc.getType() != DocumentChange.Type.ADDED) continue;
                if (dc.getDocument().getMetadata().hasPendingWrites()) continue;
                Notificacao n = dc.getDocument().toObject(Notificacao.class);
                // Só mostra no sistema notificações criadas depois que o app abriu
                if (n.data >= inicio - 5000) NotificationHelper.mostrar(ctx, n);
            }
        });
    }

    public void parar() {
        if (registro != null) registro.remove();
        registro = null;
        uidAtual = null;
        total = 0;
        avisar();
    }

    public void adicionar(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
        l.onNaoLidas(total);
    }

    public void remover(Listener l) {
        listeners.remove(l);
    }

    private void avisar() {
        for (Listener l : new ArrayList<>(listeners)) l.onNaoLidas(total);
    }
}
