package com.ferramentafacil.app.utils;

import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/** Formatação e leitura de valores em Real (R$). */
public final class MoneyUtils {

    private static final Locale BR = new Locale("pt", "BR");

    private MoneyUtils() { }

    public static String formatar(double valor) {
        return NumberFormat.getCurrencyInstance(BR).format(valor);
    }

    /** Converte textos como "35", "35,50" ou "1.234,90" em número. Retorna null se inválido. */
    @Nullable
    public static Double ler(String texto) {
        if (texto == null) return null;
        String t = texto.trim().replace("R$", "").replace(" ", "");
        if (t.isEmpty()) return null;
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        }
        try {
            double v = Double.parseDouble(t);
            // Arredonda para 2 casas (centavos)
            return Math.round(v * 100) / 100.0;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Valor para exibir num campo de texto (ex.: 35.5 → "35,50"). */
    public static String paraCampo(double valor) {
        return String.format(BR, "%.2f", valor);
    }
}
