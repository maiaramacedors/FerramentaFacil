package com.ferramentafacil.app.utils;

import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.StorageException;

import java.io.IOException;

/** Converte exceções do Firebase em mensagens amigáveis em português. */
public final class FirebaseErrorUtils {

    private FirebaseErrorUtils() { }

    public static String mensagem(Exception e) {
        if (e == null) return "Ocorreu um erro inesperado.";
        if (e instanceof RuntimeExecutionException && e.getCause() instanceof Exception) {
            return mensagem((Exception) e.getCause());
        }
        if (e instanceof FirebaseNetworkException)
            return "Sem conexão com a internet. Verifique sua rede e tente novamente.";
        if (e instanceof FirebaseTooManyRequestsException)
            return "Muitas tentativas seguidas. Aguarde alguns minutos e tente novamente.";
        if (e instanceof FirebaseAuthWeakPasswordException)
            return "Senha muito fraca. Use pelo menos 6 caracteres, com letras e números.";
        if (e instanceof FirebaseAuthInvalidUserException)
            return "Não encontramos uma conta ativa com este e-mail.";
        if (e instanceof FirebaseAuthInvalidCredentialsException)
            return "E-mail ou senha incorretos.";
        if (e instanceof FirebaseAuthUserCollisionException)
            return "Já existe uma conta cadastrada com este e-mail.";
        if (e instanceof FirebaseAuthRecentLoginRequiredException)
            return "Por segurança, saia e entre novamente antes de realizar esta ação.";

        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
            switch (fe.getCode()) {
                case INVALID_ARGUMENT:
                    // Usado pelo app para regras de negócio dentro de transações
                    return fe.getMessage();
                case PERMISSION_DENIED:
                    return "Você não tem permissão para realizar esta ação.";
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                    return "Servidor indisponível ou sem internet. Tente novamente em instantes.";
                case NOT_FOUND:
                    return "Registro não encontrado. Ele pode ter sido removido.";
                default:
                    return "Erro ao acessar os dados (" + fe.getCode().name() + ").";
            }
        }
        if (e instanceof StorageException) {
            StorageException se = (StorageException) e;
            if (se.getErrorCode() == StorageException.ERROR_NOT_AUTHENTICATED
                    || se.getErrorCode() == StorageException.ERROR_NOT_AUTHORIZED)
                return "Sem permissão para enviar a imagem. Verifique as regras do Firebase Storage.";
            if (se.getErrorCode() == StorageException.ERROR_RETRY_LIMIT_EXCEEDED)
                return "Conexão instável: não foi possível enviar a imagem.";
            return "Falha ao enviar a imagem.";
        }
        if (e instanceof IOException) return "Não foi possível ler a imagem selecionada.";
        return e.getMessage() != null ? e.getMessage() : "Ocorreu um erro inesperado.";
    }

    /** Cria um erro de regra de negócio para ser lançado dentro de transações do Firestore. */
    public static FirebaseFirestoreException regra(String mensagem) {
        return new FirebaseFirestoreException(mensagem, FirebaseFirestoreException.Code.INVALID_ARGUMENT);
    }
}
