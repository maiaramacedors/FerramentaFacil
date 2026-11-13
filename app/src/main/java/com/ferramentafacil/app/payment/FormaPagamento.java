package com.ferramentafacil.app.payment;

/**
 * Formas e status de pagamento.
 * Na versão 1 o pagamento é combinado diretamente entre as partes (A_COMBINAR);
 * PIX e CARTAO_CREDITO estão previstos para uma integração futura.
 */
public final class FormaPagamento {
    public static final String A_COMBINAR = "A_COMBINAR";
    public static final String PIX = "PIX";
    public static final String CARTAO_CREDITO = "CARTAO_CREDITO";

    public static final String STATUS_NAO_APLICAVEL = "NAO_APLICAVEL";
    public static final String STATUS_PENDENTE = "PENDENTE";
    public static final String STATUS_PAGO = "PAGO";

    private FormaPagamento() { }
}
