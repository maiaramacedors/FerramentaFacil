package com.ferramentafacil.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.model.Notificacao;
import com.ferramentafacil.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificacaoAdapter extends RecyclerView.Adapter<NotificacaoAdapter.VH> {

    public interface OnClick {
        void onClick(Notificacao n);
    }

    private final List<Notificacao> itens = new ArrayList<>();
    private final OnClick listener;

    public NotificacaoAdapter(OnClick listener) {
        this.listener = listener;
    }

    public void setItens(List<Notificacao> lista) {
        itens.clear();
        itens.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notificacao, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Notificacao n = itens.get(position);
        h.tvTitulo.setText(n.titulo);
        h.tvMensagem.setText(n.mensagem);
        h.tvData.setText(DateUtils.formatarDataHora(n.data));
        h.vNaoLida.setVisibility(n.lida ? View.INVISIBLE : View.VISIBLE);
        h.itemView.setOnClickListener(v -> listener.onClick(n));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitulo, tvMensagem, tvData;
        final View vNaoLida;

        VH(View v) {
            super(v);
            tvTitulo = v.findViewById(R.id.tvTitulo);
            tvMensagem = v.findViewById(R.id.tvMensagem);
            tvData = v.findViewById(R.id.tvData);
            vNaoLida = v.findViewById(R.id.vNaoLida);
        }
    }
}
