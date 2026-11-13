package com.ferramentafacil.app.repository;

import com.ferramentafacil.app.model.Favorite;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.FirebaseErrorUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Favoritos do usuário (coleção "favorites", ID = userId_toolId). */
public class FavoriteRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final ToolRepository toolRepo = new ToolRepository();

    private String uid() {
        return FirebaseAuth.getInstance().getUid();
    }

    private DocumentReference ref(String toolId) {
        return db.collection(Constants.COL_FAVORITES).document(uid() + "_" + toolId);
    }

    public void verificar(String toolId, Callback<Boolean> cb) {
        ref(toolId).get()
                .addOnSuccessListener(doc -> cb.onSuccess(doc.exists()))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    /** Favorita/desfavorita. Retorna o novo estado. */
    public void definir(String toolId, boolean favoritar, Callback<Boolean> cb) {
        if (favoritar) {
            Favorite f = new Favorite();
            f.userId = uid();
            f.toolId = toolId;
            f.dataCriacao = System.currentTimeMillis();
            ref(toolId).set(f)
                    .addOnSuccessListener(v -> cb.onSuccess(true))
                    .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
        } else {
            ref(toolId).delete()
                    .addOnSuccessListener(v -> cb.onSuccess(false))
                    .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
        }
    }

    /** Ferramentas favoritadas (anúncios excluídos são ignorados). */
    public void listarFerramentas(Callback<List<Tool>> cb) {
        db.collection(Constants.COL_FAVORITES).whereEqualTo("userId", uid()).get()
                .addOnSuccessListener(snap -> {
                    List<Favorite> favs = snap.toObjects(Favorite.class);
                    Collections.sort(favs, (a, b) -> Long.compare(b.dataCriacao, a.dataCriacao));
                    List<String> ids = new ArrayList<>();
                    for (Favorite f : favs) ids.add(f.toolId);
                    toolRepo.buscarVarias(ids, cb);
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }
}
