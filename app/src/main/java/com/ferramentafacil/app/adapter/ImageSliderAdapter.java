package com.ferramentafacil.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Galeria de fotos deslizante (ViewPager2) da tela de detalhes. */
public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.VH> {

    private final List<String> urls = new ArrayList<>();

    public ImageSliderAdapter(List<String> lista) {
        if (lista != null) urls.addAll(lista);
        if (urls.isEmpty()) urls.add(""); // mostra o placeholder
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_imagem_slider, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UiUtils.carregarImagem(h.iv, urls.get(position), R.drawable.placeholder_ferramenta);
    }

    @Override
    public int getItemCount() {
        return urls.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView iv;

        VH(View v) {
            super(v);
            iv = v.findViewById(R.id.ivImagem);
        }
    }
}
