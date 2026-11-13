package com.ferramentafacil.app.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Avaliação feita pelo locatário após um aluguel concluído (coleção "reviews").
 * O ID do documento é o próprio rentalId: isso garante UMA avaliação por aluguel.
 */
public class Review {
    @DocumentId
    public String reviewId;
    public String rentalId;
    public String reviewerId;
    public String reviewerNome;
    public String reviewedUserId;
    public String toolId;
    public String toolNome;
    /** Nota de 1 a 5. */
    public int nota;
    public String comentario = "";
    public long dataAvaliacao;

    public Review() { }
}
