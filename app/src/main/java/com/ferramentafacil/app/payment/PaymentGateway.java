package com.ferramentafacil.app.payment;

import com.ferramentafacil.app.model.Rental;

/**
 * Contrato para uma futura integração de pagamentos (ex.: Mercado Pago, PagSeguro, Stripe).
 *
 * IMPORTANTE: o app NÃO simula pagamentos. Para integrar de verdade, crie uma classe que
 * implemente esta interface chamando um backend seguro (Cloud Functions) — nunca coloque
 * chaves secretas de pagamento dentro do aplicativo.
 */
public interface PaymentGateway {

    interface Resultado {
        void onAprovado(String transacaoId);
        void onFalha(String mensagem);
    }

    /** Indica se pagamentos online estão disponíveis nesta versão. */
    boolean isDisponivel();

    /** Inicia o pagamento de um aluguel aprovado. */
    void iniciarPagamento(Rental aluguel, String formaPagamento, Resultado resultado);
}
