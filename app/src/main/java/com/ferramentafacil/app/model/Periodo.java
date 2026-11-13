package com.ferramentafacil.app.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Período já reservado de uma ferramenta (aluguel aprovado).
 * Fica salvo dentro do documento da ferramenta (campo periodosOcupados) para que qualquer
 * usuário consiga verificar a disponibilidade sem precisar ler os aluguéis de outras pessoas.
 * Datas em milissegundos (meia-noite UTC do dia).
 */
public class Periodo {
    public String rentalId;
    public long inicio;
    public long fim;

    public Periodo() { }

    public Periodo(String rentalId, long inicio, long fim) {
        this.rentalId = rentalId;
        this.inicio = inicio;
        this.fim = fim;
    }

    public Map<String, Object> paraMapa() {
        Map<String, Object> m = new HashMap<>();
        m.put("rentalId", rentalId);
        m.put("inicio", inicio);
        m.put("fim", fim);
        return m;
    }
}
