package com.ferramentafacil.app.repository;

import com.ferramentafacil.app.model.Notificacao;
import com.ferramentafacil.app.model.Rental;
import com.ferramentafacil.app.model.RentalStatus;
import com.ferramentafacil.app.model.Review;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.FirebaseErrorUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.List;

/**
 * Avaliações. Só é possível avaliar aluguéis CONCLUÍDOS, uma única vez (ID da avaliação = rentalId).
 * A transação grava a avaliação e atualiza as médias do proprietário e da ferramenta juntos.
 */
public class ReviewRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void avaliar(Rental r, int nota, String comentario, String autorNome, Callback<Void> cb) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (nota < 1 || nota > 5) {
            cb.onError("Escolha uma nota de 1 a 5 estrelas.");
            return;
        }
        DocumentReference reviewRef = db.collection(Constants.COL_REVIEWS).document(r.rentalId);
        DocumentReference userRef = db.collection(Constants.COL_USERS).document(r.ownerId);
        DocumentReference toolRef = db.collection(Constants.COL_TOOLS).document(r.toolId);
        DocumentReference rentalRef = db.collection(Constants.COL_RENTALS).document(r.rentalId);

        db.runTransaction(tx -> {
            DocumentSnapshot rs = tx.get(rentalRef);
            DocumentSnapshot rv = tx.get(reviewRef);
            DocumentSnapshot us = tx.get(userRef);
            DocumentSnapshot ts = tx.get(toolRef);

            if (!RentalStatus.CONCLUIDA.equals(rs.getString("status")))
                throw FirebaseErrorUtils.regra("Só é possível avaliar aluguéis concluídos.");
            if (uid == null || !uid.equals(rs.getString("renterId")))
                throw FirebaseErrorUtils.regra("Apenas quem alugou pode avaliar este aluguel.");
            if (rv.exists())
                throw FirebaseErrorUtils.regra("Este aluguel já foi avaliado.");

            Review review = new Review();
            review.rentalId = r.rentalId;
            review.reviewerId = uid;
            review.reviewerNome = autorNome;
            review.reviewedUserId = r.ownerId;
            review.toolId = r.toolId;
            review.toolNome = r.toolNome;
            review.nota = nota;
            review.comentario = comentario != null ? comentario.trim() : "";
            review.dataAvaliacao = System.currentTimeMillis();
            tx.set(reviewRef, review);

            long somaU = valor(us, "somaNotas") + nota;
            long totalU = valor(us, "totalAvaliacoes") + 1;
            tx.update(userRef, "somaNotas", somaU, "totalAvaliacoes", totalU,
                    "avaliacaoMedia", (double) somaU / totalU, "ultimaAvaliacaoId", r.rentalId);

            if (ts.exists()) {
                long somaT = valor(ts, "somaNotas") + nota;
                long totalT = valor(ts, "totalAvaliacoes") + 1;
                tx.update(toolRef, "somaNotas", somaT, "totalAvaliacoes", totalT,
                        "avaliacaoMedia", (double) somaT / totalT, "ultimaAvaliacaoId", r.rentalId);
            }
            tx.update(rentalRef, "avaliado", true);
            return null;
        }).addOnSuccessListener(v -> {
            new NotificationRepository().enviar(r.ownerId, "Você recebeu uma avaliação ⭐",
                    autorNome + " avaliou o aluguel de \"" + r.toolNome + "\" com " + nota + " estrela(s).",
                    Notificacao.TIPO_AVALIACAO, r.rentalId);
            UserRepository.limparCache();
            cb.onSuccess(null);
        }).addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    private static long valor(DocumentSnapshot d, String campo) {
        Long v = d.getLong(campo);
        return v != null ? v : 0;
    }

    public void listarDoUsuario(String uid, Callback<List<Review>> cb) {
        db.collection(Constants.COL_REVIEWS).whereEqualTo("reviewedUserId", uid).get()
                .addOnSuccessListener(snap -> {
                    List<Review> lista = snap.toObjects(Review.class);
                    Collections.sort(lista, (a, b) -> Long.compare(b.dataAvaliacao, a.dataAvaliacao));
                    cb.onSuccess(lista);
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public void listarDaFerramenta(String toolId, Callback<List<Review>> cb) {
        db.collection(Constants.COL_REVIEWS).whereEqualTo("toolId", toolId).get()
                .addOnSuccessListener(snap -> {
                    List<Review> lista = snap.toObjects(Review.class);
                    Collections.sort(lista, (a, b) -> Long.compare(b.dataAvaliacao, a.dataAvaliacao));
                    cb.onSuccess(lista);
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }
}
