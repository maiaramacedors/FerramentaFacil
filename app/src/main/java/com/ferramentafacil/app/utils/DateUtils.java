package com.ferramentafacil.app.utils;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Datas e cálculo de diárias.
 *
 * As datas de aluguel são guardadas como "meia-noite UTC" do dia escolhido — é o mesmo formato
 * devolvido pelo calendário do Material Design (MaterialDatePicker), o que evita erros de fuso.
 *
 * POLÍTICA DE LOCAÇÃO (cálculo de dias):
 *   quantidade de dias = data de devolução − data de retirada
 *   retirada e devolução no MESMO dia = 1 diária
 *   Ex.: retirada 10/03, devolução 13/03 → 3 diárias.
 * O dia da devolução fica livre para uma nova retirada (períodos "meio abertos").
 */
public final class DateUtils {

    public static final long DIA_MS = 24L * 60 * 60 * 1000;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final Locale BR = new Locale("pt", "BR");

    private DateUtils() { }

    /** Hoje (data local do aparelho) representado como meia-noite UTC. */
    public static long hojeUtc() {
        Calendar local = Calendar.getInstance();
        Calendar utc = Calendar.getInstance(UTC);
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    /** Quantidade de diárias entre retirada e devolução (mínimo 1). Retorna 0 se o período for inválido. */
    public static int calcularDias(long retirada, long devolucao) {
        if (devolucao < retirada) return 0;
        long dias = (devolucao - retirada) / DIA_MS;
        return (int) Math.max(1, dias);
    }

    public static double calcularTotal(double valorDiaria, int dias) {
        return valorDiaria * dias;
    }

    /** Valida o período escolhido. Retorna a mensagem de erro ou null se estiver tudo certo. */
    @Nullable
    public static String validarPeriodo(long retirada, long devolucao) {
        if (retirada < hojeUtc()) return "A data de retirada não pode estar no passado.";
        if (devolucao < retirada) return "A data de devolução deve ser igual ou posterior à retirada.";
        if (calcularDias(retirada, devolucao) > Constants.MAX_DIAS_ALUGUEL)
            return "O período máximo de aluguel é de " + Constants.MAX_DIAS_ALUGUEL + " dias.";
        return null;
    }

    /** Fim "exclusivo" do período: o dia da devolução não fica bloqueado. */
    private static long fimExclusivo(long inicio, long fim) {
        return Math.max(fim, inicio + DIA_MS);
    }

    /** Verifica se dois períodos se sobrepõem. */
    public static boolean sobrepoe(long aInicio, long aFim, long bInicio, long bFim) {
        return aInicio < fimExclusivo(bInicio, bFim) && bInicio < fimExclusivo(aInicio, aFim);
    }

    /** O dia informado está dentro de um período ocupado? (usado para bloquear dias no calendário) */
    public static boolean diaOcupado(long dia, long inicio, long fim) {
        return dia >= inicio && dia < fimExclusivo(inicio, fim);
    }

    public static String formatarData(long utcMillis) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", BR);
        f.setTimeZone(UTC);
        return f.format(new Date(utcMillis));
    }

    /** Data e hora no fuso do aparelho (mensagens, notificações). */
    public static String formatarDataHora(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", BR).format(new Date(millis));
    }

    public static String formatarHoraCurta(long millis) {
        Calendar agora = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        boolean hoje = agora.get(Calendar.YEAR) == c.get(Calendar.YEAR)
                && agora.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR);
        String padrao = hoje ? "HH:mm" : "dd/MM HH:mm";
        return new SimpleDateFormat(padrao, BR).format(new Date(millis));
    }
}
