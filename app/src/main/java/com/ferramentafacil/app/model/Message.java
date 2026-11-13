package com.ferramentafacil.app.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Mensagem do chat (chats/{chatId}/messages/{messageId}).
 * Cada conversa está ligada a uma ferramenta e a um locatário (chatId = toolId_renterId)
 * e, quando existir, à solicitação de aluguel (rentalId).
 */
public class Message {
    @DocumentId
    public String messageId;
    public String chatId;
    public String rentalId;
    public String toolId;
    public String senderId;
    public String senderNome;
    public String receiverId;
    public String mensagem;
    public long dataHora;

    public Message() { }
}
