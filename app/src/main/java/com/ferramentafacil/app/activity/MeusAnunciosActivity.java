package com.ferramentafacil.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.adapter.MyToolAdapter;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.ToolRepository;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.MoneyUtils;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/** Gerenciamento dos anúncios: ver, editar, alterar preço/disponibilidade, pedidos e excluir. */
public class MeusAnunciosActivity extends AppCompatActivity implements MyToolAdapter.Acoes {

    private MyToolAdapter adapter;
    private ProgressBar progress;
    private TextView tvVazio;
    private final ToolRepository toolRepo = new ToolRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Meus anúncios");
        toolbar.setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.progress);
        tvVazio = findViewById(R.id.tvVazio);
        RecyclerView rv = findViewById(R.id.rvLista);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyToolAdapter(this);
        rv.setAdapter(adapter);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(v -> startActivity(new Intent(this, CadastrarFerramentaActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        progress.setVisibility(View.VISIBLE);
        toolRepo.listarMinhas(new Callback<List<Tool>>() {
            @Override
            public void onSuccess(List<Tool> lista) {
                progress.setVisibility(View.GONE);
                adapter.setItens(lista);
                tvVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
                tvVazio.setText("Você ainda não anunciou ferramentas.\nToque em \"Novo anúncio\" para começar.");
            }

            @Override
            public void onError(String mensagem) {
                progress.setVisibility(View.GONE);
                tvVazio.setVisibility(View.VISIBLE);
                tvVazio.setText(mensagem);
            }
        });
    }

    @Override
    public void onVer(Tool t) {
        DetalhesFerramentaActivity.abrir(this, t.toolId);
    }

    @Override
    public void onEditar(Tool t) {
        startActivity(new Intent(this, CadastrarFerramentaActivity.class).putExtra(Constants.EXTRA_TOOL_ID, t.toolId));
    }

    @Override
    public void onAlterarPreco(Tool t) {
        View v = getLayoutInflater().inflate(R.layout.dialog_input, null);
        TextInputLayout til = v.findViewById(R.id.tilInput);
        TextInputEditText et = v.findViewById(R.id.etInput);
        til.setHint("Novo valor por dia (R$)");
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(MoneyUtils.paraCampo(t.valorDiaria));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Alterar preço")
                .setMessage("Aluguéis já solicitados ou aprovados mantêm o valor combinado.")
                .setView(v)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            Double valor = MoneyUtils.ler(UiUtils.texto(et));
            if (valor == null || valor <= 0 || valor > Constants.VALOR_MAXIMO_DIARIA) {
                til.setError("Valor inválido");
                return;
            }
            toolRepo.alterarPreco(t.toolId, valor, new Callback<Void>() {
                @Override
                public void onSuccess(Void r) {
                    dialog.dismiss();
                    UiUtils.toast(MeusAnunciosActivity.this, "Preço atualizado.");
                    carregar();
                }

                @Override
                public void onError(String m) {
                    til.setError(m);
                }
            });
        }));
        dialog.show();
    }

    @Override
    public void onAlterarDisponibilidade(Tool t, boolean disponivel) {
        toolRepo.alterarDisponibilidade(t.toolId, disponivel, new Callback<Void>() {
            @Override
            public void onSuccess(Void r) {
                t.disponivel = disponivel;
                UiUtils.toast(MeusAnunciosActivity.this,
                        disponivel ? "Ferramenta disponível para aluguel." : "Ferramenta marcada como indisponível.");
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String m) {
                UiUtils.toast(MeusAnunciosActivity.this, m);
                carregar();
            }
        });
    }

    @Override
    public void onSolicitacoes(Tool t) {
        startActivity(new Intent(this, SolicitacoesActivity.class)
                .putExtra(Constants.EXTRA_TOOL_ID, t.toolId)
                .putExtra(SolicitacoesActivity.EXTRA_TITULO, t.nome));
    }

    @Override
    public void onExcluir(Tool t) {
        UiUtils.confirmar(this, "Excluir anúncio?",
                "\"" + t.nome + "\" será removido definitivamente, junto com as fotos. Essa ação não pode ser desfeita.",
                "Excluir", () -> {
                    AlertDialog loading = UiUtils.carregando(this, "Excluindo...");
                    loading.show();
                    toolRepo.excluir(t, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void r) {
                            loading.dismiss();
                            UiUtils.toast(MeusAnunciosActivity.this, "Anúncio excluído.");
                            carregar();
                        }

                        @Override
                        public void onError(String m) {
                            loading.dismiss();
                            UiUtils.alerta(MeusAnunciosActivity.this, "Não foi possível excluir", m);
                        }
                    });
                });
    }
}
