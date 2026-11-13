package com.ferramentafacil.app.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Notificação interna (coleção "notifications").
 * Exibida na tela de notificações e como notificação do Android enquanto o app está aberto.
 */
public class Notificacao {
    public static final String TIPO_SOLICITACAO = "SOLICITACAO";
    public static final String TIPO_STATUS = "STATUS_ALUGUEL";
    public static final String TIPO_MENSAGEM = "MENSAGEM";
    public static final String TIPO_AVALIACAO = "AVALIACAO";

    @DocumentId
    public String id;
    public String userId;
    public String remetenteId;
    public String titulo;
    public String mensagem;
    public String tipo;
    public String refId;
    public boolean lida;
    public long data;

    public Notificacao() { }
}
