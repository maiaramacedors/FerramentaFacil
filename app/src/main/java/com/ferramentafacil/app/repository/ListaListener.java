package com.ferramentafacil.app.repository;

import java.util.List;

/** Listener de listas em tempo real (snapshot listeners do Firestore). */
public interface ListaListener<T> {
    void onLista(List<T> itens);

    void onErro(String mensagem);
}
