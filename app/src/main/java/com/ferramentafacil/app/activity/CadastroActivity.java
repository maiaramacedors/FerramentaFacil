package com.ferramentafacil.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.repository.AuthRepository;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.utils.UiUtils;
import com.ferramentafacil.app.utils.Validators;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Cadastro de nova conta (a mesma conta serve para anunciar e para alugar). */
public class CadastroActivity extends AppCompatActivity {

    private TextInputLayout tilNome, tilEmail, tilTelefone, tilCpf, tilSenha, tilConfirmar, tilCidade, tilEndereco;
    private TextInputEditText etNome, etEmail, etTelefone, etCpf, etSenha, etConfirmar, etCidade, etEndereco;
    private MaterialCheckBox cbTermos;
    private MaterialButton btnCadastrar;
    private ProgressBar progress;
    private final AuthRepository authRepo = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilNome = findViewById(R.id.tilNome);
        tilEmail = findViewById(R.id.tilEmail);
        tilTelefone = findViewById(R.id.tilTelefone);
        tilCpf = findViewById(R.id.tilCpf);
        tilSenha = findViewById(R.id.tilSenha);
        tilConfirmar = findViewById(R.id.tilConfirmarSenha);
        tilCidade = findViewById(R.id.tilCidade);
        tilEndereco = findViewById(R.id.tilEndereco);
        etNome = findViewById(R.id.etNome);
        etEmail = findViewById(R.id.etEmail);
        etTelefone = findViewById(R.id.etTelefone);
        etCpf = findViewById(R.id.etCpf);
        etSenha = findViewById(R.id.etSenha);
        etConfirmar = findViewById(R.id.etConfirmarSenha);
        etCidade = findViewById(R.id.etCidade);
        etEndereco = findViewById(R.id.etEndereco);
        cbTermos = findViewById(R.id.cbTermos);
        btnCadastrar = findViewById(R.id.btnCadastrar);
        progress = findViewById(R.id.progress);

        findViewById(R.id.tvVerTermos).setOnClickListener(v ->
                startActivity(new Intent(this, TermosActivity.class)));
        btnCadastrar.setOnClickListener(v -> cadastrar());
    }

    /** Valida todos os campos obrigatórios antes de criar a conta. */
    private boolean validar() {
        for (TextInputLayout t : new TextInputLayout[]{tilNome, tilEmail, tilTelefone, tilCpf, tilSenha,
                tilConfirmar, tilCidade, tilEndereco}) {
            t.setError(null);
        }
        boolean ok = true;
        String nome = UiUtils.texto(etNome);
        if (nome.length() < 3 || !nome.contains(" ")) {
            tilNome.setError("Informe seu nome e sobrenome");
            ok = false;
        }
        if (!Validators.emailValido(UiUtils.texto(etEmail))) {
            tilEmail.setError("E-mail inválido");
            ok = false;
        }
        if (!Validators.telefoneValido(UiUtils.texto(etTelefone))) {
            tilTelefone.setError("Telefone inválido (informe DDD + número)");
            ok = false;
        }
        String cpf = UiUtils.texto(etCpf);
        if (!cpf.isEmpty() && !Validators.cpfValido(cpf)) {
            tilCpf.setError("CPF inválido");
            ok = false;
        }
        String senha = etSenha.getText() == null ? "" : etSenha.getText().toString();
        String confirmar = etConfirmar.getText() == null ? "" : etConfirmar.getText().toString();
        if (!Validators.senhaForte(senha)) {
            tilSenha.setError("Use pelo menos 6 caracteres, com letras e números");
            ok = false;
        }
        if (!senha.equals(confirmar)) {
            tilConfirmar.setError("As senhas não conferem");
            ok = false;
        }
        if (Validators.vazio(UiUtils.texto(etCidade))) {
            tilCidade.setError("Informe sua cidade");
            ok = false;
        }
        if (Validators.vazio(UiUtils.texto(etEndereco))) {
            tilEndereco.setError("Informe seu endereço");
            ok = false;
        }
        if (ok && !cbTermos.isChecked()) {
            UiUtils.toast(this, "Para continuar, aceite os termos de uso e a política de privacidade.");
            ok = false;
        }
        return ok;
    }

    private void cadastrar() {
        if (!validar()) return;
        carregando(true);
        authRepo.cadastrar(
                UiUtils.texto(etNome),
                UiUtils.texto(etEmail),
                etSenha.getText().toString(),
                Validators.apenasDigitos(UiUtils.texto(etTelefone)),
                Validators.apenasDigitos(UiUtils.texto(etCpf)),
                UiUtils.texto(etCidade),
                UiUtils.texto(etEndereco),
                new Callback<Void>() {
                    @Override
                    public void onSuccess(Void r) {
                        carregando(false);
                        UiUtils.toast(CadastroActivity.this, "Conta criada! Bem-vindo(a) ao FerramentaFácil.");
                        Intent i = new Intent(CadastroActivity.this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    }

                    @Override
                    public void onError(String mensagem) {
                        carregando(false);
                        UiUtils.alerta(CadastroActivity.this, "Não foi possível criar a conta", mensagem);
                    }
                });
    }

    private void carregando(boolean sim) {
        progress.setVisibility(sim ? View.VISIBLE : View.GONE);
        btnCadastrar.setEnabled(!sim);
    }
}
