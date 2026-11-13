package com.ferramentafacil.app.utils;

/** Constantes usadas em todo o app (nomes de coleções, categorias, extras de Intent...). */
public final class Constants {

    // Coleções do Firestore
    public static final String COL_USERS = "users";
    public static final String COL_TOOLS = "tools";
    public static final String COL_RENTALS = "rentals";
    public static final String COL_FAVORITES = "favorites";
    public static final String COL_CHATS = "chats";
    public static final String COL_MESSAGES = "messages";
    public static final String COL_REVIEWS = "reviews";
    public static final String COL_NOTIFICATIONS = "notifications";
    public static final String SUB_PRIVADO = "privado";
    public static final String DOC_DADOS = "dados";
    public static final String DOC_ENDERECO = "endereco";

    // Categorias e estados de conservação
    public static final String[] CATEGORIAS = {
            "Elétricas", "Manuais", "Construção", "Limpeza", "Jardinagem", "Automotivas"
    };
    public static final String[] ESTADOS = {
            "Novo", "Seminovo", "Bom estado", "Usado (com marcas de uso)"
    };

    // Extras de Intent
    public static final String EXTRA_TOOL_ID = "extra_tool_id";
    public static final String EXTRA_CATEGORIA = "extra_categoria";
    public static final String EXTRA_RENTAL_ID = "extra_rental_id";
    public static final String EXTRA_USER_ID = "extra_user_id";

    // Regras de negócio
    public static final int MAX_FOTOS = 5;
    public static final int MAX_DIAS_ALUGUEL = 90;
    public static final double VALOR_MAXIMO_DIARIA = 10000;
    public static final float RAIO_PADRAO_KM = 30f;
    /** Casas decimais usadas para "aproximar" a localização pública (2 casas ≈ 1,1 km). */
    public static final int CASAS_APROXIMACAO = 2;

    private Constants() { }
}
