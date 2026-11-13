package com.ferramentafacil.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.model.Chat;
import com.ferramentafacil.app.utils.DateUtils;
import com.ferramentafacil.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Lista de conversas do usuário. */
public class ConversaAdapter extends RecyclerView.Adapter<ConversaAdapter.VH> {

    public interface OnClick {
        void onClick(Chat chat);
    }

    private final List<Chat> itens = new ArrayList<>();
    private final String meuUid;
    private final OnClick listener;

    public ConversaAdapter(String meuUid, OnClick listener) {
        this.meuUid = meuUid;
        this.listener = listener;
    }

    public void setItens(List<Chat> lista) {
        itens.clear();
        itens.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversa, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Chat c = itens.get(position);
        String outroNome = "";
        if (c.participantes != null && c.nomes != null) {
            for (String id : c.participantes) {
                if (!id.equals(meuUid) && c.nomes.get(id) != null) outroNome = c.nomes.get(id);
            }
        }
        boolean souDono = c.participantes != null && !c.participantes.isEmpty() && meuUid.equals(c.participantes.get(0));
        h.tvNome.setText(outroNome + (souDono ? " (interessado)" : " (proprietário)"));
        h.tvFerramenta.setText(c.toolNome);
        h.tvUltima.setText(c.ultimaMensagem != null ? c.ultimaMensagem : "Conversa iniciada");
        h.tvData.setText(c.atualizadoEm > 0 ? DateUtils.formatarHoraCurta(c.atualizadoEm) : "");
        UiUtils.carregarImagem(h.ivFerramenta, c.toolImagem, R.drawable.placeholder_ferramenta);
        h.itemView.setOnClickListener(v -> listener.onClick(c));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivFerramenta;
        final TextView tvNome, tvFerramenta, tvUltima, tvData;

        VH(View v) {
            super(v);
            ivFerramenta = v.findViewById(R.id.ivFerramenta);
            tvNome = v.findViewById(R.id.tvNome);
            tvFerramenta = v.findViewById(R.id.tvFerramenta);
            tvUltima = v.findViewById(R.id.tvUltima);
            tvData = v.findViewById(R.id.tvData);
        }
    }
}
