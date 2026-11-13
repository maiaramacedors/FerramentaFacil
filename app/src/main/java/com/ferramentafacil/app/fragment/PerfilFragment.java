package com.ferramentafacil.app.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.activity.AvaliacoesActivity;
import com.ferramentafacil.app.activity.ConversasActivity;
import com.ferramentafacil.app.activity.EditarPerfilActivity;
import com.ferramentafacil.app.activity.LoginActivity;
import com.ferramentafacil.app.activity.MainActivity;
import com.ferramentafacil.app.activity.MeusAnunciosActivity;
import com.ferramentafacil.app.activity.SolicitacoesActivity;
import com.ferramentafacil.app.activity.TermosActivity;
import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.model.UserPrivado;
import com.ferramentafacil.app.repository.AuthRepository;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.RentalRepository;
import com.ferramentafacil.app.repository.ToolRepository;
import com.ferramentafacil.app.repository.UserRepository;
import com.ferramentafacil.app.service.NotificacaoWatcher;
import com.ferramentafacil.app.utils.PrefsManager;
import com.ferramentafacil.app.utils.UiUtils;
import com.ferramentafacil.app.utils.Validators;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

/** Perfil: foto, dados, avaliação, números e opções da conta. */
public class PerfilFragment extends Fragment {

    private ImageView ivFoto;
    private TextView tvNome, tvCidade, tvAvaliacao, tvQtdFerramentas, tvQtdAlugueis, tvEmail, tvTelefone, tvEndereco;
    private String uid;

    private final AuthRepository authRepo = new AuthRepository();
    private final UserRepository userRepo = new UserRepository();

    private final ActivityResultLauncher<PickVisualMediaRequest> escolherFoto = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) enviarFoto(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        uid = authRepo.getUid();
        ivFoto = v.findViewById(R.id.ivFoto);
        tvNome = v.findViewById(R.id.tvNome);
        tvCidade = v.findViewById(R.id.tvCidade);
        tvAvaliacao = v.findViewById(R.id.tvAvaliacao);
        tvQtdFerramentas = v.findViewById(R.id.tvQtdFerramentas);
        tvQtdAlugueis = v.findViewById(R.id.tvQtdAlugueis);
        tvEmail = v.findViewById(R.id.tvEmail);
        tvTelefone = v.findViewById(R.id.tvTelefone);
        tvEndereco = v.findViewById(R.id.tvEndereco);

        View.OnClickListener trocarFoto = b -> escolherFoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
        ivFoto.setOnClickListener(trocarFoto);
        v.findViewById(R.id.btnTrocarFoto).setOnClickListener(trocarFoto);

        v.findViewById(R.id.optMeusAnuncios).setOnClickListener(b ->
                startActivity(new Intent(getContext(), MeusAnunciosActivity.class)));
        v.findViewById(R.id.optSolicitacoes).setOnClickListener(b ->
                startActivity(new Intent(getContext(), SolicitacoesActivity.class)));
        v.findViewById(R.id.optConversas).setOnClickListener(b ->
                startActivity(new Intent(getContext(), ConversasActivity.class)));
        v.findViewById(R.id.optHistorico).setOnClickListener(b ->
                startActivity(MainActivity.intentAba(requireContext(), MainActivity.ABA_ALUGUEIS, 3)));
        v.findViewById(R.id.optAvaliacoes).setOnClickListener(b -> AvaliacoesActivity.abrir(requireContext(), uid));
        v.findViewById(R.id.optEditar).setOnClickListener(b ->
                startActivity(new Intent(getContext(), EditarPerfilActivity.class)));
        v.findViewById(R.id.optSenha).setOnClickListener(b -> alterarSenha());
        v.findViewById(R.id.optTermos).setOnClickListener(b ->
                startActivity(new Intent(getContext(), TermosActivity.class)));
        v.findViewById(R.id.optSair).setOnClickListener(b -> sair());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) carregar();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) carregar();
    }

    private void carregar() {
        if (uid == null) return;
        tvEmail.setText(authRepo.getEmail());
        userRepo.buscar(uid, new Callback<User>() {
            @Override
            public void onSuccess(User u) {
                if (!isAdded()) return;
                tvNome.setText(u.nome);
                tvCidade.setText(u.cidade);
                tvAvaliacao.setText(u.totalAvaliacoes > 0
                        ? String.format(new Locale("pt", "BR"), "%.1f (%d avaliações)", u.media(), u.totalAvaliacoes)
                        : "Sem avaliações ainda");
                UiUtils.carregarImagem(ivFoto, u.fotoPerfil, R.drawable.placeholder_pessoa);
            }

            @Override public void onError(String m) { }
        });
        userRepo.buscarPrivado(uid, new Callback<UserPrivado>() {
            @Override
            public void onSuccess(UserPrivado p) {
                if (!isAdded()) return;
                tvTelefone.setText(formatarTelefone(p.telefone));
                tvEndereco.setText(p.endereco != null ? p.endereco : "—");
            }

            @Override public void onError(String m) { }
        });
        new ToolRepository().contarDoUsuario(uid, new Callback<Integer>() {
            @Override public void onSuccess(Integer n) { if (isAdded()) tvQtdFerramentas.setText(String.valueOf(n)); }
            @Override public void onError(String m) { }
        });
        new RentalRepository().contarConcluidos(uid, new Callback<Integer>() {
            @Override public void onSuccess(Integer n) { if (isAdded()) tvQtdAlugueis.setText(String.valueOf(n)); }
            @Override public void onError(String m) { }
        });
    }

    private static String formatarTelefone(String t) {
        String d = Validators.apenasDigitos(t);
        if (d.length() == 11) return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        if (d.length() == 10) return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
        return d.isEmpty() ? "—" : d;
    }

    private void enviarFoto(Uri uri) {
        AlertDialog loading = UiUtils.carregando(requireContext(), "Enviando foto...");
        loading.show();
        userRepo.atualizarFoto(requireContext(), uid, uri, new Callback<String>() {
            @Override
            public void onSuccess(String url) {
                loading.dismiss();
                if (isAdded()) UiUtils.carregarImagem(ivFoto, url, R.drawable.placeholder_pessoa);
            }

            @Override
            public void onError(String m) {
                loading.dismiss();
                UiUtils.toast(getContext(), m);
            }
        });
    }

    private void alterarSenha() {
        View v = getLayoutInflater().inflate(R.layout.dialog_alterar_senha, null);
        TextInputLayout tilAtual = v.findViewById(R.id.tilSenhaAtual);
        TextInputLayout tilNova = v.findViewById(R.id.tilNovaSenha);
        TextInputLayout tilConf = v.findViewById(R.id.tilConfirmar);
        TextInputEditText etAtual = v.findViewById(R.id.etSenhaAtual);
        TextInputEditText etNova = v.findViewById(R.id.etNovaSenha);
        TextInputEditText etConf = v.findViewById(R.id.etConfirmar);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Alterar senha")
                .setView(v)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Alterar", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            tilAtual.setError(null);
            tilNova.setError(null);
            tilConf.setError(null);
            String atual = etAtual.getText() == null ? "" : etAtual.getText().toString();
            String nova = etNova.getText() == null ? "" : etNova.getText().toString();
            String conf = etConf.getText() == null ? "" : etConf.getText().toString();
            if (atual.isEmpty()) { tilAtual.setError("Informe a senha atual"); return; }
            if (!Validators.senhaForte(nova)) { tilNova.setError("Mínimo 6 caracteres, com letras e números"); return; }
            if (!nova.equals(conf)) { tilConf.setError("As senhas não conferem"); return; }
            authRepo.alterarSenha(atual, nova, new Callback<Void>() {
                @Override
                public void onSuccess(Void r) {
                    dialog.dismiss();
                    UiUtils.toast(getContext(), "Senha alterada com sucesso.");
                }

                @Override
                public void onError(String m) {
                    tilAtual.setError(m);
                }
            });
        }));
        dialog.show();
    }

    private void sair() {
        UiUtils.confirmar(requireContext(), "Sair da conta?", "Você precisará entrar novamente para usar o app.",
                "Sair", () -> {
                    NotificacaoWatcher.get().parar();
                    UserRepository.limparCache();
                    new PrefsManager(requireContext()).limpar();
                    authRepo.sair();
                    Intent i = new Intent(requireContext(), LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                });
    }
}
