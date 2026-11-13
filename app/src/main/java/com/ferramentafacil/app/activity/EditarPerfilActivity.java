package com.ferramentafacil.app.activity;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.model.UserPrivado;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.UserRepository;
import com.ferramentafacil.app.utils.UiUtils;
import com.ferramentafacil.app.utils.Validators;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

/** Edição de nome, telefone, cidade e endereço. */
public class EditarPerfilActivity extends AppCompatActivity {

    private TextInputLayout tilNome, tilTelefone, tilCidade, tilEndereco;
    private TextInputEditText etNome, etTelefone, etCidade, etEndereco;
    private String uid;
    private final UserRepository userRepo = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        uid = FirebaseAuth.getInstance().getUid();

        tilNome = findViewById(R.id.tilNome);
        tilTelefone = findViewById(R.id.tilTelefone);
        tilCidade = findViewById(R.id.tilCidade);
        tilEndereco = findViewById(R.id.tilEndereco);
        etNome = findViewById(R.id.etNome);
        etTelefone = findViewById(R.id.etTelefone);
        etCidade = findViewById(R.id.etCidade);
        etEndereco = findViewById(R.id.etEndereco);
        findViewById(R.id.btnSalvar).setOnClickListener(v -> salvar());

        userRepo.buscar(uid, new Callback<User>() {
            @Override
            public void onSuccess(User u) {
                etNome.setText(u.nome);
                etCidade.setText(u.cidade);
            }

            @Override public void onError(String m) { }
        });
        userRepo.buscarPrivado(uid, new Callback<UserPrivado>() {
            @Override
            public void onSuccess(UserPrivado p) {
                etTelefone.setText(p.telefone);
                etEndereco.setText(p.endereco);
            }

            @Override public void onError(String m) { }
        });
    }

    private void salvar() {
        tilNome.setError(null);
        tilTelefone.setError(null);
        tilCidade.setError(null);
        tilEndereco.setError(null);
        String nome = UiUtils.texto(etNome);
        String tel = UiUtils.texto(etTelefone);
        String cidade = UiUtils.texto(etCidade);
        String end = UiUtils.texto(etEndereco);
        boolean ok = true;
        if (nome.length() < 3 || !nome.contains(" ")) { tilNome.setError("Informe nome e sobrenome"); ok = false; }
        if (!Validators.telefoneValido(tel)) { tilTelefone.setError("Telefone inválido"); ok = false; }
        if (cidade.isEmpty()) { tilCidade.setError("Informe a cidade"); ok = false; }
        if (end.isEmpty()) { tilEndereco.setError("Informe o endereço"); ok = false; }
        if (!ok) return;

        AlertDialog loading = UiUtils.carregando(this, "Salvando...");
        loading.show();
        userRepo.atualizarPerfil(uid, nome, cidade, Validators.apenasDigitos(tel), end, new Callback<Void>() {
            @Override
            public void onSuccess(Void r) {
                loading.dismiss();
                UiUtils.toast(EditarPerfilActivity.this, "Perfil atualizado.");
                finish();
            }

            @Override
            public void onError(String m) {
                loading.dismiss();
                UiUtils.alerta(EditarPerfilActivity.this, "Erro", m);
            }
        });
    }
}
