package com.ferramentafacil.app.repository;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.model.UserPrivado;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.FirebaseErrorUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Leitura e atualização dos perfis de usuário. */
public class UserRepository {

    /** Cache em memória dos perfis públicos (evita buscar o mesmo proprietário várias vezes). */
    private static final Map<String, User> CACHE = new HashMap<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final StorageRepository storage = new StorageRepository();

    public static void limparCache() {
        CACHE.clear();
    }

    private DocumentReference ref(String uid) {
        return db.collection(Constants.COL_USERS).document(uid);
    }

    public void buscar(String uid, Callback<User> cb) {
        ref(uid).get()
                .addOnSuccessListener(doc -> {
                    User u = doc.toObject(User.class);
                    if (u == null) {
                        cb.onError("Perfil não encontrado.");
                        return;
                    }
                    CACHE.put(uid, u);
                    cb.onSuccess(u);
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public void buscarPrivado(String uid, Callback<UserPrivado> cb) {
        ref(uid).collection(Constants.SUB_PRIVADO).document(Constants.DOC_DADOS).get()
                .addOnSuccessListener(doc -> {
                    UserPrivado p = doc.toObject(UserPrivado.class);
                    cb.onSuccess(p != null ? p : new UserPrivado());
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    /** Busca vários perfis de uma vez (consultas "whereIn" em blocos de 10 IDs). */
    public Task<Map<String, User>> buscarVarios(Collection<String> ids) {
        Map<String, User> resultado = new HashMap<>();
        List<String> faltando = new ArrayList<>();
        for (String id : ids) {
            if (id == null) continue;
            if (CACHE.containsKey(id)) resultado.put(id, CACHE.get(id));
            else faltando.add(id);
        }
        if (faltando.isEmpty()) return Tasks.forResult(resultado);

        List<Task<QuerySnapshot>> tarefas = new ArrayList<>();
        for (int i = 0; i < faltando.size(); i += 10) {
            List<String> bloco = new ArrayList<>(faltando.subList(i, Math.min(i + 10, faltando.size())));
            tarefas.add(db.collection(Constants.COL_USERS).whereIn(FieldPath.documentId(), bloco).get());
        }
        return Tasks.whenAllComplete(tarefas).continueWith(t -> {
            for (Task<QuerySnapshot> tq : tarefas) {
                if (!tq.isSuccessful() || tq.getResult() == null) continue;
                for (DocumentSnapshot d : tq.getResult().getDocuments()) {
                    User u = d.toObject(User.class);
                    if (u != null) {
                        CACHE.put(d.getId(), u);
                        resultado.put(d.getId(), u);
                    }
                }
            }
            return resultado;
        });
    }

    /** Atualiza o perfil (dados públicos + privados). */
    public void atualizarPerfil(String uid, String nome, String cidade, String telefone, String endereco,
                                Callback<Void> cb) {
        Map<String, Object> publico = new HashMap<>();
        publico.put("nome", nome);
        publico.put("cidade", cidade);

        Map<String, Object> privado = new HashMap<>();
        privado.put("telefone", telefone);
        privado.put("endereco", endereco);

        WriteBatch b = db.batch();
        b.update(ref(uid), publico);
        b.set(ref(uid).collection(Constants.SUB_PRIVADO).document(Constants.DOC_DADOS), privado, SetOptions.merge());
        b.commit()
                .addOnSuccessListener(v -> {
                    CACHE.remove(uid);
                    cb.onSuccess(null);
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    /** Converte a nova foto de perfil (comprimida) e salva no documento do usuário. */
    public void atualizarFoto(Context ctx, String uid, Uri foto, Callback<String> cb) {
        storage.converter(ctx, foto, StorageRepository.LADO_PERFIL, StorageRepository.QUALIDADE_PERFIL)
                .addOnSuccessListener(url -> ref(uid).update("fotoPerfil", url)
                        .addOnSuccessListener(v -> {
                            CACHE.remove(uid);
                            cb.onSuccess(url);
                        })
                        .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e))))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    @Nullable
    public static User doCache(String uid) {
        return CACHE.get(uid);
    }
}
