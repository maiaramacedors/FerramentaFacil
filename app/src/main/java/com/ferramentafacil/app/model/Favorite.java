package com.ferramentafacil.app.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Ferramenta favoritada (coleção "favorites").
 * ID do documento = userId_toolId (evita favoritos duplicados).
 */
public class Favorite {
    @DocumentId
    public String favoriteId;
    public String userId;
    public String toolId;
    public long dataCriacao;

    public Favorite() { }
}
