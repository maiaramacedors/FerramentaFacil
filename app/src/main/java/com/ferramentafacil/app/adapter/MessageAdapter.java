package com.ferramentafacil.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.model.Message;
import com.ferramentafacil.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

/** Mensagens do chat: enviadas à direita (laranja), recebidas à esquerda (branco). */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    private static final int ENVIADA = 1;
    private static final int RECEBIDA = 2;

    private final String meuUid;
    private final List<Message> itens = new ArrayList<>();

    public MessageAdapter(String meuUid) {
        this.meuUid = meuUid;
    }

    public void setItens(List<Message> lista) {
        itens.clear();
        itens.addAll(lista);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return meuUid.equals(itens.get(position).senderId) ? ENVIADA : RECEBIDA;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == ENVIADA ? R.layout.item_mensagem_enviada : R.layout.item_mensagem_recebida;
        return new VH(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Message m = itens.get(position);
        h.tvMensagem.setText(m.mensagem);
        h.tvHora.setText(DateUtils.formatarDataHora(m.dataHora));
        if (h.tvRemetente != null) h.tvRemetente.setText(m.senderNome);
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvMensagem, tvHora, tvRemetente;

        VH(View v) {
            super(v);
            tvMensagem = v.findViewById(R.id.tvMensagem);
            tvHora = v.findViewById(R.id.tvHora);
            tvRemetente = v.findViewById(R.id.tvRemetente); // só existe no layout de recebida
        }
    }
}
