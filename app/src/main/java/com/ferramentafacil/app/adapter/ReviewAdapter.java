package com.ferramentafacil.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.model.Review;
import com.ferramentafacil.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

/** Lista de avaliações (perfil do proprietário e detalhes da ferramenta). */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {

    private final List<Review> itens = new ArrayList<>();
    private final boolean mostrarFerramenta;

    public ReviewAdapter(boolean mostrarFerramenta) {
        this.mostrarFerramenta = mostrarFerramenta;
    }

    public void setItens(List<Review> lista) {
        itens.clear();
        itens.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avaliacao, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Review r = itens.get(position);
        h.tvAutor.setText(r.reviewerNome);
        h.tvData.setText(DateUtils.formatarDataHora(r.dataAvaliacao).substring(0, 10));
        h.rbNota.setRating(r.nota);
        h.tvFerramenta.setVisibility(mostrarFerramenta ? View.VISIBLE : View.GONE);
        h.tvFerramenta.setText(r.toolNome);
        boolean temComentario = r.comentario != null && !r.comentario.isEmpty();
        h.tvComentario.setVisibility(temComentario ? View.VISIBLE : View.GONE);
        h.tvComentario.setText(r.comentario);
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvAutor, tvData, tvFerramenta, tvComentario;
        final RatingBar rbNota;

        VH(View v) {
            super(v);
            tvAutor = v.findViewById(R.id.tvAutor);
            tvData = v.findViewById(R.id.tvData);
            tvFerramenta = v.findViewById(R.id.tvFerramenta);
            tvComentario = v.findViewById(R.id.tvComentario);
            rbNota = v.findViewById(R.id.rbNota);
        }
    }
}
