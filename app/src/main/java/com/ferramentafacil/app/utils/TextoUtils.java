package com.ferramentafacil.app.utils;

import java.text.Normalizer;
import java.util.Locale;

/** Funções de texto usadas na pesquisa. */
public final class TextoUtils {

    private TextoUtils() { }

    /** "Furadeira de Impacto" → "furadeira de impacto"; "Elétricas" → "eletricas". */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }

    /** Verifica se TODAS as palavras da busca aparecem no texto. */
    public static boolean contemTodas(String texto, String busca) {
        String t = normalizar(texto);
        for (String palavra : normalizar(busca).split("\\s+")) {
            if (!palavra.isEmpty() && !t.contains(palavra)) return false;
        }
        return true;
    }

    public static String seguro(String s) {
        return s == null ? "" : s;
    }
}
