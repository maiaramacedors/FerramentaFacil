package com.ferramentafacil.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.repository.AuthRepository;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.utils.UiUtils;
import com.ferramentafacil.app.utils.Validators;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Login com e-mail e senha + recuperação de senha. */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilSenha;
    private TextInputEditText etEmail, etSenha;
    private MaterialButton btnEntrar;
    private ProgressBar progress;
    private final AuthRepository authRepo = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilEmail = findViewById(R.id.tilEmail);
        tilSenha = findViewById(R.id.tilSenha);
        etEmail = findViewById(R.id.etEmail);
        etSenha = findViewById(R.id.etSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        progress = findViewById(R.id.progress);

        btnEntrar.setOnClickListener(v -> entrar());
        etSenha.setOnEditorActionListener((v, actionId, event) -> {
            entrar();
            return true;
        });
        findViewById(R.id.btnCriarConta).setOnClickListener(v ->
                startActivity(new Intent(this, CadastroActivity.class)));
        findViewById(R.id.tvEsqueciSenha).setOnClickListener(v -> recuperarSenha());
    }

    private void entrar() {
        String email = UiUtils.texto(etEmail);
        String senha = etSenha.getText() == null ? "" : etSenha.getText().toString();
        tilEmail.setError(null);
        tilSenha.setError(null);

        boolean ok = true;
        if (!Validators.emailValido(email)) {
            tilEmail.setError("Informe um e-mail válido");
            ok = false;
        }
        if (senha.isEmpty()) {
            tilSenha.setError("Informe sua senha");
            ok = false;
        }
        if (!ok) return;

        carregando(true);
        authRepo.entrar(email, senha, new Callback<Void>() {
            @Override
            public void onSuccess(Void r) {
                carregando(false);
                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }

            @Override
            public void onError(String mensagem) {
                carregando(false);
                UiUtils.alerta(LoginActivity.this, "Não foi possível entrar", mensagem);
            }
        });
    }

    private void recuperarSenha() {
        View v = getLayoutInflater().inflate(R.layout.dialog_input, null);
        TextInputLayout til = v.findViewById(R.id.tilInput);
        TextInputEditText et = v.findViewById(R.id.etInput);
        til.setHint("E-mail da conta");
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        et.setText(UiUtils.texto(etEmail));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Recuperar senha")
                .setMessage("Enviaremos um link para você criar uma nova senha.")
                .setView(v)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String email = UiUtils.texto(et);
            if (!Validators.emailValido(email)) {
                til.setError("E-mail inválido");
                return;
            }
            authRepo.recuperarSenha(email, new Callback<Void>() {
                @Override
                public void onSuccess(Void r) {
                    dialog.dismiss();
                    UiUtils.alerta(LoginActivity.this, "E-mail enviado",
                            "Verifique sua caixa de entrada (e o spam) para redefinir a senha.");
                }

                @Override
                public void onError(String mensagem) {
                    til.setError(mensagem);
                }
            });
        }));
        dialog.show();
    }

    private void carregando(boolean sim) {
        progress.setVisibility(sim ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(!sim);
    }
}
