package com.ferramentafacil.app.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Miniaturas das fotos no cadastro de ferramenta.
 * Os itens podem ser String (foto já publicada, em Base64) ou Uri (foto nova da galeria/câmera).
 */
public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.VH> {

    public interface OnAlteracao {
        void onAlterou(int total);
    }

    private final List<Object> itens = new ArrayList<>();
    private final List<String> removidas = new ArrayList<>();
    private final OnAlteracao listener;

    public ImagePreviewAdapter(OnAlteracao listener) {
        this.listener = listener;
    }

    public void adicionar(Object item) {
        itens.add(item);
        notifyItemInserted(itens.size() - 1);
        listener.onAlterou(itens.size());
    }

    public void setItens(List<?> lista) {
        itens.clear();
        itens.addAll(lista);
        notifyDataSetChanged();
        listener.onAlterou(itens.size());
    }

    public void inserirNoInicio(List<?> lista) {
        itens.addAll(0, lista);
        notifyDataSetChanged();
        listener.onAlterou(itens.size());
    }

    public List<Object> getItens() {
        return itens;
    }

    /** Fotos já publicadas que continuam no anúncio. */
    public List<String> urlsExistentes() {
        List<String> r = new ArrayList<>();
        for (Object o : itens) if (o instanceof String) r.add((String) o);
        return r;
    }

    /** Fotos novas a enviar. */
    public List<Uri> urisNovas() {
        List<Uri> r = new ArrayList<>();
        for (Object o : itens) if (o instanceof Uri) r.add((Uri) o);
        return r;
    }

    /** Fotos publicadas removidas durante a edição. */
    public List<String> urlsRemovidas() {
        return removidas;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_imagem_preview, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Object item = itens.get(position);
        UiUtils.carregarImagem(h.ivFoto, item, R.drawable.placeholder_ferramenta);
        h.tvCapa.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        h.btnRemover.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Object removido = itens.remove(pos);
            if (removido instanceof String) removidas.add((String) removido);
            notifyDataSetChanged();
            listener.onAlterou(itens.size());
        });
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivFoto;
        final ImageButton btnRemover;
        final TextView tvCapa;

        VH(View v) {
            super(v);
            ivFoto = v.findViewById(R.id.ivFoto);
            btnRemover = v.findViewById(R.id.btnRemover);
            tvCapa = v.findViewById(R.id.tvCapa);
        }
    }
}
