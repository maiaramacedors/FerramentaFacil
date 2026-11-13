package com.ferramentafacil.app.model;

import com.ferramentafacil.app.R;

/**
 * Status possíveis de um aluguel e regras de agrupamento nas abas.
 *
 * Fluxo: PENDENTE → APROVADA → EM_ANDAMENTO → CONCLUIDA
 *        PENDENTE → RECUSADA (proprietário) | CANCELADA (locatário)
 *        APROVADA → CANCELADA (proprietário)
 */
public final class RentalStatus {
    public static final String PENDENTE = "PENDENTE";
    public static final String APROVADA = "APROVADA";
    public static final String EM_ANDAMENTO = "EM_ANDAMENTO";
    public static final String CONCLUIDA = "CONCLUIDA";
    public static final String RECUSADA = "RECUSADA";
    public static final String CANCELADA = "CANCELADA";

    /** Abas da tela "Meus aluguéis" (locatário). */
    public static final String[] ABAS_LOCATARIO = {
            "Aguardando aprovação", "Aprovados", "Em andamento", "Concluídos", "Cancelados"
    };
    /** Abas da tela "Solicitações recebidas" (proprietário). */
    public static final String[] ABAS_PROPRIETARIO = {
            "Pendentes", "Aprovadas", "Em andamento", "Histórico"
    };

    private RentalStatus() { }

    public static String rotulo(String status) {
        if (status == null) return "";
        switch (status) {
            case PENDENTE: return "Aguardando aprovação";
            case APROVADA: return "Aprovada";
            case EM_ANDAMENTO: return "Em andamento";
            case CONCLUIDA: return "Concluída";
            case RECUSADA: return "Recusada";
            case CANCELADA: return "Cancelada";
            default: return status;
        }
    }

    public static int cor(String status) {
        if (status == null) return R.color.cinza_status;
        switch (status) {
            case PENDENTE: return R.color.amarelo;
            case APROVADA: return R.color.verde;
            case EM_ANDAMENTO: return R.color.azul;
            case CONCLUIDA: return R.color.laranja;
            case RECUSADA:
            case CANCELADA: return R.color.vermelho;
            default: return R.color.cinza_status;
        }
    }

    public static int abaLocatario(String status) {
        if (PENDENTE.equals(status)) return 0;
        if (APROVADA.equals(status)) return 1;
        if (EM_ANDAMENTO.equals(status)) return 2;
        if (CONCLUIDA.equals(status)) return 3;
        return 4; // recusada ou cancelada
    }

    public static int abaProprietario(String status) {
        if (PENDENTE.equals(status)) return 0;
        if (APROVADA.equals(status)) return 1;
        if (EM_ANDAMENTO.equals(status)) return 2;
        return 3; // concluída, recusada ou cancelada
    }

    public static boolean ativo(String status) {
        return PENDENTE.equals(status) || APROVADA.equals(status) || EM_ANDAMENTO.equals(status);
    }
}
