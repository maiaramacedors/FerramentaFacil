package com.ferramentafacil.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.utils.LocationUtils;
import com.ferramentafacil.app.utils.MoneyUtils;
import com.ferramentafacil.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Cards de ferramentas (grade ou lista horizontal). Cada card abre a tela de detalhes. */
public class ToolAdapter extends RecyclerView.Adapter<ToolAdapter.VH> {

    public interface OnToolClick {
        void onClick(Tool tool);
    }

    private final List<Tool> itens = new ArrayList<>();
    private final boolean horizontal;
    private final OnToolClick listener;

    public ToolAdapter(boolean horizontal, OnToolClick listener) {
        this.horizontal = horizontal;
        this.listener = listener;
    }

    public void setItens(List<Tool> lista) {
        itens.clear();
        itens.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ferramenta, parent, false);
        if (horizontal) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.width = UiUtils.dp(parent.getContext(), 170);
            v.setLayoutParams(lp);
        }
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Tool t = itens.get(position);
        UiUtils.carregarImagem(h.ivFoto, t.primeiraImagem(), R.drawable.placeholder_ferramenta);
        h.tvNome.setText(t.nome);
        h.tvCategoria.setText(t.categoria);
        h.tvPreco.setText(MoneyUtils.formatar(t.valorDiaria) + "/dia");
        h.tvDistancia.setText(t.distanciaKm >= 0
                ? LocationUtils.formatarDistancia(t.distanciaKm)
                : (t.cidade != null ? t.cidade : ""));
        h.tvAvaliacao.setText(UiUtils.formatarNota(t.ownerNota, t.ownerTotalAvaliacoes));
        h.tvIndisponivel.setVisibility(t.disponivel ? View.GONE : View.VISIBLE);
        h.itemView.setOnClickListener(v -> listener.onClick(t));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivFoto;
        final TextView tvNome, tvPreco, tvDistancia, tvAvaliacao, tvCategoria, tvIndisponivel;

        VH(View v) {
            super(v);
            ivFoto = v.findViewById(R.id.ivFoto);
            tvNome = v.findViewById(R.id.tvNome);
            tvPreco = v.findViewById(R.id.tvPreco);
            tvDistancia = v.findViewById(R.id.tvDistancia);
            tvAvaliacao = v.findViewById(R.id.tvAvaliacao);
            tvCategoria = v.findViewById(R.id.tvCategoria);
            tvIndisponivel = v.findViewById(R.id.tvIndisponivel);
        }
    }
}
