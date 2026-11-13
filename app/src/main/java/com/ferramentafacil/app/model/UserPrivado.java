package com.ferramentafacil.app.model;

/**
 * Dados pessoais PRIVADOS do usuário (users/{uid}/privado/dados).
 * As regras do Firestore só permitem que o próprio dono leia/escreva este documento.
 */
public class UserPrivado {
    public String email;
    public String telefone;
    public String cpf;
    public String endereco;

    public UserPrivado() { }
}
