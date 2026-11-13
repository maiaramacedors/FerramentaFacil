package com.ferramentafacil.app.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.model.Rental;
import com.ferramentafacil.app.model.RentalStatus;
import com.ferramentafacil.app.utils.DateUtils;
import com.ferramentafacil.app.utils.MoneyUtils;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de aluguéis. O mesmo adapter atende:
 *  - MODO_LOCATARIO    → "Meus aluguéis" (cancelar solicitação, avaliar, ver endereço de retirada)
 *  - MODO_PROPRIETARIO → "Solicitações recebidas" (aceitar, recusar, confirmar retirada/devolução)
 */
public class RentalAdapter extends RecyclerView.Adapter<RentalAdapter.VH> {

    public static final int MODO_LOCATARIO = 0;
    public static final int MODO_PROPRIETARIO = 1;

    /** Ações dos botões. Métodos com implementação vazia por padrão. */
    public interface Acoes {
        default void onAceitar(Rental r) { }
        default void onRecusar(Rental r) { }
        default void onIniciar(Rental r) { }
        default void onConcluir(Rental r) { }
        default void onCancelar(Rental r) { }
        default void onAvaliar(Rental r) { }
        default void onChat(Rental r) { }
        default void onAbrirFerramenta(Rental r) { }
    }

    private final int modo;
    private final Acoes acoes;
    private final List<Rental> itens = new ArrayList<>();

    public RentalAdapter(int modo, Acoes acoes) {
        this.modo = modo;
        this.acoes = acoes;
    }

    public void setItens(List<Rental> lista) {
        itens.clear();
        itens.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_aluguel, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Rental r = itens.get(position);
        Context c = h.itemView.getContext();

        UiUtils.carregarImagem(h.ivFerramenta, r.toolImagem, R.drawable.placeholder_ferramenta);
        h.tvFerramenta.setText(r.toolNome);

        if (modo == MODO_PROPRIETARIO) {
            h.ivPessoa.setVisibility(View.VISIBLE);
            UiUtils.carregarImagem(h.ivPessoa, r.renterFoto, R.drawable.placeholder_pessoa);
            h.tvPessoa.setText("Interessado: " + nome(r.renterNome));
        } else {
            h.ivPessoa.setVisibility(View.GONE);
            h.tvPessoa.setText("Proprietário: " + nome(r.ownerNome));
        }

        h.tvStatus.setText(RentalStatus.rotulo(r.status));
        ViewCompat.setBackgroundTintList(h.tvStatus,
                ColorStateList.valueOf(ContextCompat.getColor(c, RentalStatus.cor(r.status))));

        h.tvDatas.setText("Retirada " + DateUtils.formatarData(r.dataRetirada)
                + "  →  Devolução " + DateUtils.formatarData(r.dataDevolucao));
        h.tvValores.setText(r.quantidadeDias + (r.quantidadeDias == 1 ? " dia" : " dias") + " × "
                + MoneyUtils.formatar(r.valorDiaria) + " = " + MoneyUtils.formatar(r.valorTotal));

        // Endereço de retirada só aparece para o locatário depois da aprovação
        boolean mostrarEndereco = modo == MODO_LOCATARIO
                && (RentalStatus.APROVADA.equals(r.status) || RentalStatus.EM_ANDAMENTO.equals(r.status))
                && r.enderecoRetirada != null && !r.enderecoRetirada.isEmpty();
        h.tvEndereco.setVisibility(mostrarEndereco ? View.VISIBLE : View.GONE);
        if (mostrarEndereco) h.tvEndereco.setText("Retirada em: " + r.enderecoRetirada);

        configurarBotoes(h, r);
        h.btnChat.setOnClickListener(v -> acoes.onChat(r));
        h.itemView.setOnClickListener(v -> acoes.onAbrirFerramenta(r));
    }

    private void configurarBotoes(VH h, Rental r) {
        String status = r.status == null ? "" : r.status;
        botao(h.btnPrimario, null, null);
        botao(h.btnSecundario, null, null);

        if (modo == MODO_PROPRIETARIO) {
            switch (status) {
                case RentalStatus.PENDENTE:
                    botao(h.btnPrimario, "ACEITAR SOLICITAÇÃO", () -> acoes.onAceitar(r));
                    botao(h.btnSecundario, "RECUSAR SOLICITAÇÃO", () -> acoes.onRecusar(r));
                    break;
                case RentalStatus.APROVADA:
                    botao(h.btnPrimario, "Confirmar retirada", () -> acoes.onIniciar(r));
                    botao(h.btnSecundario, "Cancelar aluguel", () -> acoes.onCancelar(r));
                    break;
                case RentalStatus.EM_ANDAMENTO:
                    botao(h.btnPrimario, "Confirmar devolução", () -> acoes.onConcluir(r));
                    break;
                default:
                    break;
            }
        } else {
            if (RentalStatus.PENDENTE.equals(status)) {
                botao(h.btnSecundario, "Cancelar solicitação", () -> acoes.onCancelar(r));
            } else if (RentalStatus.CONCLUIDA.equals(status) && !r.avaliado) {
                botao(h.btnPrimario, "Avaliar aluguel", () -> acoes.onAvaliar(r));
            }
        }
    }

    private void botao(MaterialButton b, String texto, Runnable acao) {
        if (texto == null) {
            b.setVisibility(View.GONE);
            b.setOnClickListener(null);
            return;
        }
        b.setVisibility(View.VISIBLE);
        b.setText(texto);
        b.setOnClickListener(v -> acao.run());
    }

    private static String nome(String n) {
        return n == null || n.isEmpty() ? "—" : n;
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivFerramenta, ivPessoa;
        final TextView tvFerramenta, tvPessoa, tvStatus, tvDatas, tvValores, tvEndereco;
        final MaterialButton btnPrimario, btnSecundario;
        final ImageButton btnChat;

        VH(View v) {
            super(v);
            ivFerramenta = v.findViewById(R.id.ivFerramenta);
            ivPessoa = v.findViewById(R.id.ivPessoa);
            tvFerramenta = v.findViewById(R.id.tvFerramenta);
            tvPessoa = v.findViewById(R.id.tvPessoa);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvDatas = v.findViewById(R.id.tvDatas);
            tvValores = v.findViewById(R.id.tvValores);
            tvEndereco = v.findViewById(R.id.tvEndereco);
            btnPrimario = v.findViewById(R.id.btnPrimario);
            btnSecundario = v.findViewById(R.id.btnSecundario);
            btnChat = v.findViewById(R.id.btnChat);
        }
    }
}
