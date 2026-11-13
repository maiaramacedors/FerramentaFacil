package com.ferramentafacil.app.utils;

import android.util.Patterns;

/** Validações dos formulários (cadastro, anúncio etc.). */
public final class Validators {

    private Validators() { }

    public static boolean vazio(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String apenasDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    public static boolean emailValido(String email) {
        return !vazio(email) && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /** Telefone brasileiro com DDD: 10 (fixo) ou 11 (celular) dígitos. */
    public static boolean telefoneValido(String telefone) {
        int n = apenasDigitos(telefone).length();
        return n == 10 || n == 11;
    }

    /** Senha com pelo menos 6 caracteres, contendo letra e número. */
    public static boolean senhaForte(String senha) {
        if (senha == null || senha.length() < 6) return false;
        boolean letra = false, numero = false;
        for (char c : senha.toCharArray()) {
            if (Character.isLetter(c)) letra = true;
            if (Character.isDigit(c)) numero = true;
        }
        return letra && numero;
    }

    /** Valida CPF pelo cálculo dos dígitos verificadores. */
    public static boolean cpfValido(String cpf) {
        String d = apenasDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) return false;
        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) soma += (d.charAt(i) - '0') * (10 - i);
            int dv1 = 11 - (soma % 11);
            if (dv1 >= 10) dv1 = 0;
            soma = 0;
            for (int i = 0; i < 10; i++) soma += (d.charAt(i) - '0') * (11 - i);
            int dv2 = 11 - (soma % 11);
            if (dv2 >= 10) dv2 = 0;
            return dv1 == d.charAt(9) - '0' && dv2 == d.charAt(10) - '0';
        } catch (Exception e) {
            return false;
        }
    }
}
