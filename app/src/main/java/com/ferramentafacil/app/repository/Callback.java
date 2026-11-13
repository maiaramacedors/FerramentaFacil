package com.ferramentafacil.app.repository;

/**
 * Retorno assíncrono das operações com o Firebase.
 * As chamadas ao Firebase não bloqueiam a tela: o resultado chega depois, por aqui.
 */
public interface Callback<T> {
    void onSuccess(T resultado);

    void onError(String mensagem);
}
