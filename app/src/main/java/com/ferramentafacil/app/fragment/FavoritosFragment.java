package com.ferramentafacil.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.activity.DetalhesFerramentaActivity;
import com.ferramentafacil.app.adapter.ToolAdapter;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.FavoriteRepository;
import com.ferramentafacil.app.utils.LocationUtils;
import com.ferramentafacil.app.utils.PrefsManager;

import java.util.List;

/** Ferramentas favoritadas (salvas na conta do usuário no Firebase). */
public class FavoritosFragment extends Fragment {

    private ToolAdapter adapter;
    private ProgressBar progress;
    private TextView tvVazio;
    private SwipeRefreshLayout swipe;
    private final FavoriteRepository favRepo = new FavoriteRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        return inflater.inflate(R.layout.fragment_favoritos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        progress = view.findViewById(R.id.progress);
        tvVazio = view.findViewById(R.id.tvVazio);
        swipe = view.findViewById(R.id.swipeRefresh);
        swipe.setColorSchemeResources(R.color.laranja);
        swipe.setOnRefreshListener(this::carregar);

        RecyclerView rv = view.findViewById(R.id.rvFavoritos);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ToolAdapter(false, t -> DetalhesFerramentaActivity.abrir(requireContext(), t.toolId));
        rv.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) carregar();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) carregar(); // recarrega ao voltar para a aba
    }

    private void carregar() {
        if (adapter.getItemCount() == 0 && !swipe.isRefreshing()) progress.setVisibility(View.VISIBLE);
        favRepo.listarFerramentas(new Callback<List<Tool>>() {
            @Override
            public void onSuccess(List<Tool> lista) {
                if (!isAdded()) return;
                PrefsManager prefs = new PrefsManager(requireContext());
                if (prefs.temLocalizacao()) {
                    for (Tool t : lista) {
                        t.distanciaKm = LocationUtils.distanciaKm(prefs.getLatitude(), prefs.getLongitude(),
                                t.latitude, t.longitude);
                    }
                }
                progress.setVisibility(View.GONE);
                swipe.setRefreshing(false);
                adapter.setItens(lista);
                tvVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String mensagem) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                swipe.setRefreshing(false);
                tvVazio.setVisibility(View.VISIBLE);
                tvVazio.setText(mensagem);
            }
        });
    }
}
