package com.ferramentafacil.app.model;

import com.google.firebase.firestore.DocumentId;

import java.util.List;
import java.util.Map;

/**
 * Cabeçalho de uma conversa (coleção "chats").
 * participantes = [ownerId, renterId] (sempre nesta ordem).
 */
public class Chat {
    @DocumentId
    public String chatId;
    public String toolId;
    public String toolNome;
    public String toolImagem;
    public String rentalId;
    public List<String> participantes;
    public Map<String, String> nomes;
    public String ultimaMensagem;
    public long atualizadoEm;

    public Chat() { }
}
