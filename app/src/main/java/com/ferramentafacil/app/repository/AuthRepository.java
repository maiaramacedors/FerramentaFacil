package com.ferramentafacil.app.repository;

import androidx.annotation.Nullable;

import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.model.UserPrivado;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.FirebaseErrorUtils;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

/** Cadastro, login, recuperação e troca de senha (Firebase Authentication). */
public class AuthRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    public String getUid() {
        FirebaseUser u = auth.getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    @Nullable
    public String getEmail() {
        FirebaseUser u = auth.getCurrentUser();
        return u != null ? u.getEmail() : null;
    }

    public boolean estaLogado() {
        return auth.getCurrentUser() != null;
    }

    public void entrar(String email, String senha, Callback<Void> cb) {
        auth.signInWithEmailAndPassword(email.trim(), senha)
                .addOnSuccessListener(r -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    /**
     * Cria a conta no Authentication e grava o perfil no Firestore:
     *  - users/{uid}               → dados públicos (nome, cidade, avaliação)
     *  - users/{uid}/privado/dados → e-mail, telefone, CPF e endereço (somente o dono lê)
     * Se a gravação no banco falhar, a conta recém-criada é removida para não ficar "pela metade".
     */
    public void cadastrar(String nome, String email, String senha, String telefone, String cpf,
                          String cidade, String endereco, Callback<Void> cb) {
        auth.createUserWithEmailAndPassword(email.trim(), senha)
                .addOnSuccessListener(resultado -> {
                    FirebaseUser fu = resultado.getUser();
                    if (fu == null) {
                        cb.onError("Não foi possível criar a conta.");
                        return;
                    }
                    fu.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(nome).build());

                    User user = new User();
                    user.nome = nome.trim();
                    user.cidade = cidade.trim();
                    user.fotoPerfil = "";
                    user.verificado = false;
                    user.dataCadastro = System.currentTimeMillis();

                    UserPrivado privado = new UserPrivado();
                    privado.email = email.trim();
                    privado.telefone = telefone;
                    privado.cpf = cpf;
                    privado.endereco = endereco.trim();

                    DocumentReference ref = db.collection(Constants.COL_USERS).document(fu.getUid());
                    WriteBatch batch = db.batch();
                    batch.set(ref, user);
                    batch.set(ref.collection(Constants.SUB_PRIVADO).document(Constants.DOC_DADOS), privado);
                    batch.commit()
                            .addOnSuccessListener(v -> cb.onSuccess(null))
                            .addOnFailureListener(e -> {
                                fu.delete();
                                cb.onError(FirebaseErrorUtils.mensagem(e));
                            });
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public void recuperarSenha(String email, Callback<Void> cb) {
        auth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    /** Troca a senha: o Firebase exige reautenticar com a senha atual antes. */
    public void alterarSenha(String senhaAtual, String novaSenha, Callback<Void> cb) {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null || u.getEmail() == null) {
            cb.onError("Sessão expirada. Entre novamente.");
            return;
        }
        AuthCredential cred = EmailAuthProvider.getCredential(u.getEmail(), senhaAtual);
        u.reauthenticate(cred)
                .continueWithTask(t -> {
                    if (!t.isSuccessful() && t.getException() != null) throw t.getException();
                    return u.updatePassword(novaSenha);
                })
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public void sair() {
        auth.signOut();
    }
}
