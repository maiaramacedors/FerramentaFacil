package com.ferramentafacil.app.model;

import com.ferramentafacil.app.payment.FormaPagamento;
import com.google.firebase.firestore.DocumentId;

/**
 * Solicitação/aluguel de ferramenta (coleção "rentals").
 * Guarda uma cópia do nome/foto da ferramenta e das pessoas para exibir listas rapidamente
 * e para manter o histórico mesmo que o anúncio seja alterado depois.
 * O valorDiaria fica "congelado" no momento da solicitação: se o proprietário mudar o preço,
 * os aluguéis já solicitados/confirmados não são afetados.
 */
public class Rental {
    @DocumentId
    public String rentalId;
    public String toolId;
    public String toolNome;
    public String toolImagem;
    public String ownerId;
    public String ownerNome;
    public String renterId;
    public String renterNome;
    public String renterFoto;

    /** Datas em milissegundos (meia-noite UTC). */
    public long dataRetirada;
    public long dataDevolucao;
    public int quantidadeDias;
    public double valorDiaria;
    public double valorTotal;

    /** Ver {@link RentalStatus}. */
    public String status;
    public long dataSolicitacao;
    public long dataAtualizacao;

    /** Endereço de retirada: preenchido somente quando o proprietário aprova. */
    public String enderecoRetirada;
    public boolean avaliado;

    /** Preparação para pagamentos futuros (Pix/cartão). Na v1 o valor é combinado entre as partes. */
    public String formaPagamento = FormaPagamento.A_COMBINAR;
    public String statusPagamento = FormaPagamento.STATUS_NAO_APLICAVEL;

    public Rental() { }
}
