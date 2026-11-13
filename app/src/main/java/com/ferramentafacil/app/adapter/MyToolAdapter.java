package com.ferramentafacil.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.utils.MoneyUtils;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

/** Gerenciamento dos anúncios do proprietário. */
public class MyToolAdapter extends RecyclerView.Adapter<MyToolAdapter.VH> {

    public interface Acoes {
        void onVer(Tool t);
        void onEditar(Tool t);
        void onAlterarPreco(Tool t);
        void onAlterarDisponibilidade(Tool t, boolean disponivel);
        void onSolicitacoes(Tool t);
        void onExcluir(Tool t);
    }

    private final List<Tool> itens = new ArrayList<>();
    private final Acoes acoes;

    public MyToolAdapter(Acoes acoes) {
        this.acoes = acoes;
    }

    public void setItens(List<Tool> lista) {
        itens.clear();
        itens.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meu_anuncio, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Tool t = itens.get(position);
        UiUtils.carregarImagem(h.ivFoto, t.primeiraImagem(), R.drawable.placeholder_ferramenta);
        h.tvNome.setText(t.nome);
        h.tvPreco.setText(MoneyUtils.formatar(t.valorDiaria) + "/dia");
        h.tvInfo.setText((t.disponivel ? "Disponível" : "Indisponível") + " · "
                + (t.totalAvaliacoes > 0 ? "★ " + UiUtils.formatarNota(t.media(), t.totalAvaliacoes) : "Sem avaliações"));

        // Remove o listener antes de mudar o estado para não disparar a ação sem querer
        h.swDisponivel.setOnCheckedChangeListener(null);
        h.swDisponivel.setChecked(t.disponivel);
        h.swDisponivel.setOnCheckedChangeListener((b, marcado) -> acoes.onAlterarDisponibilidade(t, marcado));

        h.btnVer.setOnClickListener(v -> acoes.onVer(t));
        h.btnEditar.setOnClickListener(v -> acoes.onEditar(t));
        h.btnPreco.setOnClickListener(v -> acoes.onAlterarPreco(t));
        h.btnSolicitacoes.setOnClickListener(v -> acoes.onSolicitacoes(t));
        h.btnExcluir.setOnClickListener(v -> acoes.onExcluir(t));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivFoto;
        final TextView tvNome, tvPreco, tvInfo;
        final MaterialSwitch swDisponivel;
        final MaterialButton btnVer, btnEditar, btnPreco, btnSolicitacoes;
        final ImageButton btnExcluir;

        VH(View v) {
            super(v);
            ivFoto = v.findViewById(R.id.ivFoto);
            tvNome = v.findViewById(R.id.tvNome);
            tvPreco = v.findViewById(R.id.tvPreco);
            tvInfo = v.findViewById(R.id.tvInfo);
            swDisponivel = v.findViewById(R.id.swDisponivel);
            btnVer = v.findViewById(R.id.btnVer);
            btnEditar = v.findViewById(R.id.btnEditar);
            btnPreco = v.findViewById(R.id.btnPreco);
            btnSolicitacoes = v.findViewById(R.id.btnSolicitacoes);
            btnExcluir = v.findViewById(R.id.btnExcluir);
        }
    }
}
