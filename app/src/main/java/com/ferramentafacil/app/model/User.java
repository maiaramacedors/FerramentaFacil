package com.ferramentafacil.app.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/**
 * Perfil PÚBLICO do usuário (coleção "users").
 * Dados sensíveis (e-mail, telefone, CPF e endereço) ficam em users/{uid}/privado/dados
 * — ver {@link UserPrivado} — para não serem expostos a outros usuários.
 *
 * Os campos são públicos para que o Firestore faça o mapeamento automático (toObject).
 */
public class User {
    @DocumentId
    public String userId;
    public String nome;
    public String fotoPerfil;
    public String cidade;

    /** Soma de todas as notas recebidas e quantidade de avaliações (média = soma / total). */
    public long somaNotas;
    public long totalAvaliacoes;
    public double avaliacaoMedia;
    public String ultimaAvaliacaoId;

    /** Indicador de perfil verificado (alterado apenas pelo administrador no console). */
    public boolean verificado;
    public long dataCadastro;

    public User() { }

    @Exclude
    public double media() {
        return totalAvaliacoes > 0 ? (double) somaNotas / totalAvaliacoes : 0;
    }

    @Exclude
    public String primeiroNome() {
        if (nome == null || nome.trim().isEmpty()) return "";
        return nome.trim().split("\\s+")[0];
    }
}
