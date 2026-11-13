package com.ferramentafacil.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.utils.Constants;

/** Lista horizontal de categorias da Home. */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface OnCategoriaClick {
        void onClick(String categoria);
    }

    private final OnCategoriaClick listener;

    public CategoryAdapter(OnCategoriaClick listener) {
        this.listener = listener;
    }

    /** Ícone de cada categoria. */
    public static int icone(String categoria) {
        if (categoria == null) return R.drawable.ic_build;
        switch (categoria) {
            case "Elétricas": return R.drawable.ic_bolt;
            case "Construção": return R.drawable.ic_domain;
            case "Limpeza": return R.drawable.ic_drop;
            case "Jardinagem": return R.drawable.ic_eco;
            case "Automotivas": return R.drawable.ic_car;
            case "Manuais":
            default: return R.drawable.ic_build;
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_categoria, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String cat = Constants.CATEGORIAS[position];
        h.tvNome.setText(cat);
        h.ivIcone.setImageResource(icone(cat));
        h.itemView.setOnClickListener(v -> listener.onClick(cat));
    }

    @Override
    public int getItemCount() {
        return Constants.CATEGORIAS.length;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivIcone;
        final TextView tvNome;

        VH(View v) {
            super(v);
            ivIcone = v.findViewById(R.id.ivIcone);
            tvNome = v.findViewById(R.id.tvNome);
        }
    }
}
