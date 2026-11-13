package com.ferramentafacil.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.adapter.ReviewAdapter;
import com.ferramentafacil.app.model.Review;
import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.ReviewRepository;
import com.ferramentafacil.app.repository.UserRepository;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

/** Média e histórico de avaliações recebidas por um usuário. */
public class AvaliacoesActivity extends AppCompatActivity {

    public static void abrir(Context c, String userId) {
        c.startActivity(new Intent(c, AvaliacoesActivity.class).putExtra(Constants.EXTRA_USER_ID, userId));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avaliacoes);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());

        String userId = getIntent().getStringExtra(Constants.EXTRA_USER_ID);
        if (userId == null) {
            finish();
            return;
        }
        ProgressBar progress = findViewById(R.id.progress);
        TextView tvVazio = findViewById(R.id.tvVazio);
        RecyclerView rv = findViewById(R.id.rvLista);
        rv.setLayoutManager(new LinearLayoutManager(this));
        ReviewAdapter adapter = new ReviewAdapter(true);
        rv.setAdapter(adapter);

        new UserRepository().buscar(userId, new Callback<User>() {
            @Override
            public void onSuccess(User u) {
                ((TextView) findViewById(R.id.tvNome)).setText(u.nome);
                ((TextView) findViewById(R.id.tvMedia)).setText(UiUtils.formatarNota(u.media(), u.totalAvaliacoes));
                ((RatingBar) findViewById(R.id.rbMedia)).setRating((float) u.media());
                ((TextView) findViewById(R.id.tvTotal)).setText(u.totalAvaliacoes + " avaliação(ões)");
                UiUtils.carregarImagem((ImageView) findViewById(R.id.ivFoto), u.fotoPerfil, R.drawable.placeholder_pessoa);
            }

            @Override public void onError(String m) { }
        });

        new ReviewRepository().listarDoUsuario(userId, new Callback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> lista) {
                progress.setVisibility(View.GONE);
                adapter.setItens(lista);
                tvVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String m) {
                progress.setVisibility(View.GONE);
                tvVazio.setVisibility(View.VISIBLE);
                tvVazio.setText(m);
            }
        });
    }
}
