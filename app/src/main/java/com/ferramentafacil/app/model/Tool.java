package com.ferramentafacil.app.model;

import androidx.annotation.Nullable;

import com.ferramentafacil.app.utils.DateUtils;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Ferramenta anunciada (coleção "tools").
 *
 * Privacidade: latitude/longitude guardadas aqui são APROXIMADAS (arredondadas, ~1 km).
 * O endereço completo e as coordenadas exatas ficam em tools/{toolId}/privado/endereco,
 * visível apenas para o proprietário (e copiado para o aluguel quando ele é aprovado).
 */
public class Tool {
    @DocumentId
    public String toolId;
    public String ownerId;
    public String nome;
    /** Nome normalizado (minúsculo, sem acentos) — facilita a pesquisa. */
    public String nomeBusca;
    public String categoria;
    public String marca;
    public String modelo;
    public String descricao;
    public String estadoConservacao;
    public List<String> imagens = new ArrayList<>();
    public double valorDiaria;
    public double latitude;
    public double longitude;
    public String cidade;
    public String bairro;
    public boolean disponivel = true;
    public long dataCadastro;
    public long dataAtualizacao;

    // Avaliações da ferramenta
    public long somaNotas;
    public long totalAvaliacoes;
    public double avaliacaoMedia;
    public String ultimaAvaliacaoId;

    /** Períodos com aluguel aprovado/em andamento (impede reservas conflitantes). */
    public List<Periodo> periodosOcupados = new ArrayList<>();

    // ---- Campos calculados no app (não são gravados no Firestore) ----
    @Exclude public double distanciaKm = -1;
    @Exclude public String ownerNome;
    @Exclude public String ownerFoto;
    @Exclude public double ownerNota;
    @Exclude public long ownerTotalAvaliacoes;
    @Exclude public boolean ownerVerificado;

    public Tool() { }

    @Exclude
    @Nullable
    public String primeiraImagem() {
        return (imagens != null && !imagens.isEmpty()) ? imagens.get(0) : null;
    }

    @Exclude
    public double media() {
        return totalAvaliacoes > 0 ? (double) somaNotas / totalAvaliacoes : 0;
    }

    /**
     * Verifica se o período [inicio, fim] conflita com algum aluguel já aprovado.
     * @param ignorarRentalId aluguel que deve ser desconsiderado (ex.: o próprio que está sendo aprovado)
     */
    @Exclude
    public boolean temConflito(long inicio, long fim, @Nullable String ignorarRentalId) {
        if (periodosOcupados == null) return false;
        for (Periodo p : periodosOcupados) {
            if (ignorarRentalId != null && ignorarRentalId.equals(p.rentalId)) continue;
            if (DateUtils.sobrepoe(inicio, fim, p.inicio, p.fim)) return true;
        }
        return false;
    }

    /** Retorna apenas os períodos que ainda não terminaram (limpa reservas antigas). */
    @Exclude
    public List<Periodo> periodosVigentes() {
        List<Periodo> lista = new ArrayList<>();
        long hoje = DateUtils.hojeUtc();
        if (periodosOcupados != null) {
            for (Periodo p : periodosOcupados) {
                if (p.fim >= hoje) lista.add(p);
            }
        }
        return lista;
    }

    @Exclude
    public String localizacaoAproximada() {
        StringBuilder sb = new StringBuilder();
        if (bairro != null && !bairro.trim().isEmpty()) sb.append(bairro.trim()).append(", ");
        sb.append(cidade != null ? cidade : "");
        return sb.toString();
    }
}
