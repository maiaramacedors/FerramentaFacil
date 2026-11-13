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

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.model.Rental;
import com.ferramentafacil.app.model.RentalStatus;
import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.RentalRepository;
import com.ferramentafacil.app.repository.ReviewRepository;
import com.ferramentafacil.app.repository.UserRepository;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

/** Avaliação (1 a 5 estrelas + comentário) de um aluguel CONCLUÍDO. */
public class AvaliarActivity extends AppCompatActivity {

    private static final String[] DESCRICOES = {
            "Toque nas estrelas para dar sua nota", "Muito ruim", "Ruim", "Regular", "Boa", "Excelente!"
    };

    public static void abrir(Context c, String rentalId) {
        c.startActivity(new Intent(c, AvaliarActivity.class).putExtra(Constants.EXTRA_RENTAL_ID, rentalId));
    }

    private RatingBar ratingBar;
    private TextInputEditText etComentario;
    private MaterialButton btnEnviar;
    private ProgressBar progress;
    private Rental aluguel;
    private String meuNome = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avaliar);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());

        ratingBar = findViewById(R.id.ratingBar);
        etComentario = findViewById(R.id.etComentario);
        btnEnviar = findViewById(R.id.btnEnviar);
        progress = findViewById(R.id.progress);
        TextView tvNota = findViewById(R.id.tvNotaDescricao);
        ratingBar.setOnRatingBarChangeListener((rb, nota, user) -> tvNota.setText(DESCRICOES[Math.round(nota)]));
        btnEnviar.setOnClickListener(v -> enviar());
        btnEnviar.setEnabled(false);

        String uid = FirebaseAuth.getInstance().getUid();
        new UserRepository().buscar(uid, new Callback<User>() {
            @Override public void onSuccess(User u) { meuNome = u.nome; }
            @Override public void onError(String m) { }
        });

        String rentalId = getIntent().getStringExtra(Constants.EXTRA_RENTAL_ID);
        new RentalRepository().buscar(rentalId, new Callback<Rental>() {
            @Override
            public void onSuccess(Rental r) {
                if (!RentalStatus.CONCLUIDA.equals(r.status) || r.avaliado || !r.renterId.equals(uid)) {
                    UiUtils.toast(AvaliarActivity.this, "Este aluguel não pode ser avaliado (somente aluguéis concluídos, uma vez).");
                    finish();
                    return;
                }
                aluguel = r;
                UiUtils.carregarImagem((ImageView) findViewById(R.id.ivFerramenta), r.toolImagem, R.drawable.placeholder_ferramenta);
                ((TextView) findViewById(R.id.tvFerramenta)).setText(r.toolNome);
                ((TextView) findViewById(R.id.tvProprietario)).setText("Proprietário: " + r.ownerNome);
                btnEnviar.setEnabled(true);
            }

            @Override
            public void onError(String m) {
                UiUtils.toast(AvaliarActivity.this, m);
                finish();
            }
        });
    }

    private void enviar() {
        int nota = Math.round(ratingBar.getRating());
        if (nota < 1) {
            UiUtils.toast(this, "Escolha de 1 a 5 estrelas.");
            return;
        }
        progress.setVisibility(View.VISIBLE);
        btnEnviar.setEnabled(false);
        new ReviewRepository().avaliar(aluguel, nota, UiUtils.texto(etComentario), meuNome, new Callback<Void>() {
            @Override
            public void onSuccess(Void r) {
                UiUtils.toast(AvaliarActivity.this, "Obrigado pela avaliação!");
                finish();
            }

            @Override
            public void onError(String m) {
                progress.setVisibility(View.GONE);
                btnEnviar.setEnabled(true);
                UiUtils.alerta(AvaliarActivity.this, "Não foi possível avaliar", m);
            }
        });
    }
}
