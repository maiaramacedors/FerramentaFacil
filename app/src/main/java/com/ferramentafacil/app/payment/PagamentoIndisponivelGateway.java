package com.ferramentafacil.app.payment;

import com.ferramentafacil.app.model.Rental;

/**
 * Implementação usada na versão 1: informa que pagamentos online ainda não existem.
 * Basta trocar por uma implementação real quando a integração for feita.
 */
public class PagamentoIndisponivelGateway implements PaymentGateway {

    @Override
    public boolean isDisponivel() {
        return false;
    }

    @Override
    public void iniciarPagamento(Rental aluguel, String formaPagamento, Resultado resultado) {
        resultado.onFalha("Pagamentos pelo aplicativo ainda não estão disponíveis. "
                + "Combine o pagamento diretamente com o proprietário.");
    }
}
