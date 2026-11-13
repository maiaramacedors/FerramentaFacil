package com.ferramentafacil.app.activity;

import android.content.ActivityNotFoundException;
import android.location.Address;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.adapter.ImagePreviewAdapter;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.StorageRepository;
import com.ferramentafacil.app.repository.ToolRepository;
import com.ferramentafacil.app.repository.UserRepository;
import com.ferramentafacil.app.service.LocationHelper;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.LocationUtils;
import com.ferramentafacil.app.utils.MoneyUtils;
import com.ferramentafacil.app.utils.UiUtils;
import com.ferramentafacil.app.utils.Validators;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cadastro (e edição) de anúncio de ferramenta: fotos da galeria/câmera, dados, preço,
 * localização (GPS ou endereço digitado), disponibilidade, prévia e PUBLICAR FERRAMENTA.
 * Salva os dados no Firestore e as imagens no Firebase Storage.
 */
public class CadastrarFerramentaActivity extends AppCompatActivity {

    private TextInputLayout tilNome, tilCategoria, tilDescricao, tilEstado, tilValor, tilCidade, tilEndereco;
    private TextInputEditText etNome, etMarca, etModelo, etDescricao, etValor, etCidade, etBairro, etEndereco;
    private MaterialAutoCompleteTextView actCategoria, actEstado;
    private MaterialSwitch swDisponivel;
    private TextView tvCoordenadas, tvSemFotos;
    private MaterialButton btnPublicar;
    private ImagePreviewAdapter fotosAdapter;

    private String toolIdEdicao;
    private Tool toolOriginal;
    private Double latExata, lngExata;
    private boolean preenchendo = false;
    private Uri fotoCameraUri;

    private final ToolRepository toolRepo = new ToolRepository();

    // Galeria (seletor de fotos do Android) — várias imagens de uma vez
    private final ActivityResultLauncher<PickVisualMediaRequest> galeria = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(Constants.MAX_FOTOS), uris -> {
                for (Uri u : uris) {
                    if (fotosAdapter.getItemCount() >= Constants.MAX_FOTOS) {
                        UiUtils.toast(this, "Limite de " + Constants.MAX_FOTOS + " fotos atingido.");
                        break;
                    }
                    fotosAdapter.adicionar(u);
                }
            });

    // Câmera — salva a foto no arquivo indicado por fotoCameraUri
    private final ActivityResultLauncher<Uri> camera = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (Boolean.TRUE.equals(ok) && fotoCameraUri != null) fotosAdapter.adicionar(fotoCameraUri);
            });

    private final ActivityResultLauncher<String[]> permissaoLocal = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), r -> {
                if (LocationHelper.temPermissao(this)) usarLocalizacaoAtual();
                else UiUtils.toast(this, "Sem permissão de localização. Digite o endereço manualmente.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar_ferramenta);
        ligarViews();

        toolIdEdicao = getIntent().getStringExtra(Constants.EXTRA_TOOL_ID);
        if (savedInstanceState != null) restaurar(savedInstanceState);

        if (toolIdEdicao != null) {
            ((MaterialToolbar) findViewById(R.id.toolbar)).setTitle("Editar anúncio");
            btnPublicar.setText("SALVAR ALTERAÇÕES");
            if (savedInstanceState == null) carregarParaEdicao();
        } else if (savedInstanceState == null) {
            preencherCidadeDoPerfil();
        }
    }

    private void ligarViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilNome = findViewById(R.id.tilNome);
        tilCategoria = findViewById(R.id.tilCategoria);
        tilDescricao = findViewById(R.id.tilDescricao);
        tilEstado = findViewById(R.id.tilEstado);
        tilValor = findViewById(R.id.tilValor);
        tilCidade = findViewById(R.id.tilCidade);
        tilEndereco = findViewById(R.id.tilEndereco);
        etNome = findViewById(R.id.etNome);
        etMarca = findViewById(R.id.etMarca);
        etModelo = findViewById(R.id.etModelo);
        etDescricao = findViewById(R.id.etDescricao);
        etValor = findViewById(R.id.etValor);
        etCidade = findViewById(R.id.etCidade);
        etBairro = findViewById(R.id.etBairro);
        etEndereco = findViewById(R.id.etEndereco);
        actCategoria = findViewById(R.id.actCategoria);
        actEstado = findViewById(R.id.actEstado);
        swDisponivel = findViewById(R.id.swDisponivel);
        tvCoordenadas = findViewById(R.id.tvCoordenadas);
        tvSemFotos = findViewById(R.id.tvSemFotos);
        btnPublicar = findViewById(R.id.btnPublicar);

        actCategoria.setSimpleItems(Constants.CATEGORIAS);
        actEstado.setSimpleItems(Constants.ESTADOS);

        RecyclerView rvFotos = findViewById(R.id.rvFotos);
        rvFotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        fotosAdapter = new ImagePreviewAdapter(total -> tvSemFotos.setVisibility(total == 0 ? View.VISIBLE : View.GONE));
        rvFotos.setAdapter(fotosAdapter);

        findViewById(R.id.btnGaleria).setOnClickListener(v -> {
            if (fotosAdapter.getItemCount() >= Constants.MAX_FOTOS) {
                UiUtils.toast(this, "Limite de " + Constants.MAX_FOTOS + " fotos atingido.");
                return;
            }
            galeria.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
        });
        findViewById(R.id.btnCamera).setOnClickListener(v -> abrirCamera());
        findViewById(R.id.btnUsarLocalizacao).setOnClickListener(v -> {
            if (LocationHelper.temPermissao(this)) usarLocalizacaoAtual();
            else permissaoLocal.launch(LocationHelper.PERMISSOES);
        });
        findViewById(R.id.btnPreview).setOnClickListener(v -> {
            if (validar()) mostrarPrevia();
        });
        btnPublicar.setOnClickListener(v -> {
            if (validar()) publicar();
        });

        // Se o usuário editar cidade/endereço à mão, as coordenadas do GPS deixam de valer
        TextWatcher invalidaGps = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (!preenchendo) {
                    latExata = null;
                    lngExata = null;
                    tvCoordenadas.setVisibility(View.GONE);
                }
            }
        };
        etCidade.addTextChangedListener(invalidaGps);
        etEndereco.addTextChangedListener(invalidaGps);
    }

    // ------------------------------------------------------------ Estado (rotação de tela)

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        // Guarda só as fotos NOVAS (Uri). As já publicadas são grandes demais para o Bundle
        // e são recarregadas do banco em restaurar().
        ArrayList<String> fotos = new ArrayList<>();
        for (Object o : fotosAdapter.getItens()) if (o instanceof Uri) fotos.add(o.toString());
        out.putStringArrayList("fotos", fotos);
        if (fotoCameraUri != null) out.putString("fotoCamera", fotoCameraUri.toString());
        if (latExata != null) {
            out.putDouble("lat", latExata);
            out.putDouble("lng", lngExata);
        }
    }

    private void restaurar(Bundle s) {
        ArrayList<String> fotos = s.getStringArrayList("fotos");
        if (fotos != null) {
            List<Object> itens = new ArrayList<>();
            for (String f : fotos) itens.add(Uri.parse(f));
            fotosAdapter.setItens(itens);
        }
        if (toolIdEdicao != null) recarregarFotosPublicadas();
        String cam = s.getString("fotoCamera");
        if (cam != null) fotoCameraUri = Uri.parse(cam);
        if (s.containsKey("lat")) {
            latExata = s.getDouble("lat");
            lngExata = s.getDouble("lng");
            tvCoordenadas.setVisibility(View.VISIBLE);
        }
    }

    /** Após rotação de tela na edição: busca de novo as fotos já publicadas. */
    private void recarregarFotosPublicadas() {
        toolRepo.buscarPorId(toolIdEdicao, new Callback<Tool>() {
            @Override
            public void onSuccess(Tool t) {
                toolOriginal = t;
                if (t.imagens != null) fotosAdapter.inserirNoInicio(t.imagens);
            }

            @Override public void onError(String m) { }
        });
    }

    // ------------------------------------------------------------ Fotos e localização

    private void abrirCamera() {
        if (fotosAdapter.getItemCount() >= Constants.MAX_FOTOS) {
            UiUtils.toast(this, "Limite de " + Constants.MAX_FOTOS + " fotos atingido.");
            return;
        }
        try {
            File pasta = new File(getCacheDir(), "images");
            if (!pasta.exists()) pasta.mkdirs();
            File arquivo = File.createTempFile("foto_", ".jpg", pasta);
            fotoCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", arquivo);
            camera.launch(fotoCameraUri);
        } catch (IOException | ActivityNotFoundException e) {
            UiUtils.toast(this, "Não foi possível abrir a câmera.");
        }
    }

    private void usarLocalizacaoAtual() {
        AlertDialog loading = UiUtils.carregando(this, "Obtendo sua localização...");
        loading.show();
        LocationHelper.obterLocalizacaoAtual(this, new LocationHelper.ResultadoLocalizacao() {
            @Override
            public void onLocalizacao(double lat, double lng) {
                LocationHelper.enderecoDe(CadastrarFerramentaActivity.this, lat, lng, endereco -> {
                    loading.dismiss();
                    preenchendo = true;
                    if (endereco != null) {
                        String cidade = LocationHelper.cidade(endereco);
                        if (cidade != null) etCidade.setText(LocationHelper.descreverCidade(endereco));
                        if (endereco.getSubLocality() != null) etBairro.setText(endereco.getSubLocality());
                        if (endereco.getAddressLine(0) != null) etEndereco.setText(endereco.getAddressLine(0));
                    }
                    preenchendo = false;
                    latExata = lat;
                    lngExata = lng;
                    tvCoordenadas.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onErro(String mensagem) {
                loading.dismiss();
                UiUtils.toast(CadastrarFerramentaActivity.this, mensagem);
            }
        });
    }

    private void preencherCidadeDoPerfil() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        new UserRepository().buscar(uid, new Callback<User>() {
            @Override
            public void onSuccess(User u) {
                if (UiUtils.texto(etCidade).isEmpty() && u.cidade != null) {
                    preenchendo = true;
                    etCidade.setText(u.cidade);
                    preenchendo = false;
                }
            }

            @Override public void onError(String m) { }
        });
    }

    // ------------------------------------------------------------ Edição

    private void carregarParaEdicao() {
        AlertDialog loading = UiUtils.carregando(this, "Carregando anúncio...");
        loading.show();
        toolRepo.buscarPorId(toolIdEdicao, new Callback<Tool>() {
            @Override
            public void onSuccess(Tool t) {
                loading.dismiss();
                if (!t.ownerId.equals(FirebaseAuth.getInstance().getUid())) {
                    UiUtils.toast(CadastrarFerramentaActivity.this, "Você só pode editar os seus anúncios.");
                    finish();
                    return;
                }
                toolOriginal = t;
                preenchendo = true;
                etNome.setText(t.nome);
                actCategoria.setText(t.categoria, false);
                etMarca.setText(t.marca);
                etModelo.setText(t.modelo);
                etDescricao.setText(t.descricao);
                actEstado.setText(t.estadoConservacao, false);
                etValor.setText(MoneyUtils.paraCampo(t.valorDiaria));
                etCidade.setText(t.cidade);
                etBairro.setText(t.bairro);
                swDisponivel.setChecked(t.disponivel);
                preenchendo = false;
                fotosAdapter.setItens(t.imagens != null ? t.imagens : new ArrayList<>());

                toolRepo.buscarEndereco(toolIdEdicao, new Callback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> dados) {
                        preenchendo = true;
                        Object end = dados.get("endereco");
                        if (end != null) etEndereco.setText(end.toString());
                        preenchendo = false;
                        Object lat = dados.get("latitudeExata"), lng = dados.get("longitudeExata");
                        if (lat instanceof Number && lng instanceof Number) {
                            latExata = ((Number) lat).doubleValue();
                            lngExata = ((Number) lng).doubleValue();
                        }
                    }

                    @Override public void onError(String m) { }
                });
            }

            @Override
            public void onError(String mensagem) {
                loading.dismiss();
                UiUtils.toast(CadastrarFerramentaActivity.this, mensagem);
                finish();
            }
        });
    }

    // ------------------------------------------------------------ Validação, prévia e publicação

    private boolean validar() {
        for (TextInputLayout t : new TextInputLayout[]{tilNome, tilCategoria, tilDescricao, tilEstado, tilValor,
                tilCidade, tilEndereco}) {
            t.setError(null);
        }
        boolean ok = true;
        if (UiUtils.texto(etNome).length() < 3) {
            tilNome.setError("Informe o nome da ferramenta (mínimo 3 letras)");
            ok = false;
        }
        if (!Arrays.asList(Constants.CATEGORIAS).contains(actCategoria.getText().toString())) {
            tilCategoria.setError("Selecione uma categoria");
            ok = false;
        }
        if (UiUtils.texto(etDescricao).length() < 10) {
            tilDescricao.setError("Descreva a ferramenta (mínimo 10 caracteres)");
            ok = false;
        }
        if (!Arrays.asList(Constants.ESTADOS).contains(actEstado.getText().toString())) {
            tilEstado.setError("Selecione o estado de conservação");
            ok = false;
        }
        Double valor = MoneyUtils.ler(UiUtils.texto(etValor));
        if (valor == null || valor <= 0) {
            tilValor.setError("Informe um valor válido");
            ok = false;
        } else if (valor > Constants.VALOR_MAXIMO_DIARIA) {
            tilValor.setError("Valor máximo: " + MoneyUtils.formatar(Constants.VALOR_MAXIMO_DIARIA));
            ok = false;
        }
        if (Validators.vazio(UiUtils.texto(etCidade))) {
            tilCidade.setError("Informe a cidade");
            ok = false;
        }
        if (Validators.vazio(UiUtils.texto(etEndereco))) {
            tilEndereco.setError("Informe o endereço de retirada");
            ok = false;
        }
        if (ok && fotosAdapter.getItemCount() == 0) {
            UiUtils.toast(this, "Adicione pelo menos uma foto da ferramenta.");
            ok = false;
        }
        return ok;
    }

    /** Monta o objeto com os dados do formulário. */
    private Tool montarTool() {
        Tool t = toolOriginal != null ? toolOriginal : new Tool();
        t.nome = UiUtils.texto(etNome);
        t.categoria = actCategoria.getText().toString();
        t.marca = UiUtils.texto(etMarca);
        t.modelo = UiUtils.texto(etModelo);
        t.descricao = UiUtils.texto(etDescricao);
        t.estadoConservacao = actEstado.getText().toString();
        Double v = MoneyUtils.ler(UiUtils.texto(etValor));
        t.valorDiaria = v != null ? v : 0;
        t.cidade = UiUtils.texto(etCidade);
        t.bairro = UiUtils.texto(etBairro);
        t.disponivel = swDisponivel.isChecked();
        return t;
    }

    private void mostrarPrevia() {
        Tool t = montarTool();
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_preview_anuncio, null);
        dialog.setContentView(v);

        UiUtils.carregarImagem((ImageView) v.findViewById(R.id.ivPrevFoto),
                fotosAdapter.getItens().get(0), R.drawable.placeholder_ferramenta);
        ((TextView) v.findViewById(R.id.tvPrevCategoria)).setText(t.categoria);
        ((TextView) v.findViewById(R.id.tvPrevNome)).setText(t.nome);
        String marca = (t.marca + " " + t.modelo).trim();
        TextView tvMarca = v.findViewById(R.id.tvPrevMarca);
        tvMarca.setText(marca);
        tvMarca.setVisibility(marca.isEmpty() ? View.GONE : View.VISIBLE);
        ((TextView) v.findViewById(R.id.tvPrevPreco)).setText(MoneyUtils.formatar(t.valorDiaria) + " / dia");
        ((TextView) v.findViewById(R.id.tvPrevEstado)).setText("Estado: " + t.estadoConservacao
                + " · " + fotosAdapter.getItemCount() + " foto(s)");
        ((TextView) v.findViewById(R.id.tvPrevLocal)).setText(t.localizacaoAproximada() + " (localização aproximada)");
        ((TextView) v.findViewById(R.id.tvPrevDescricao)).setText(t.descricao);
        TextView tvDisp = v.findViewById(R.id.tvPrevDisponivel);
        tvDisp.setText(t.disponivel ? "✓ Disponível para aluguel" : "✗ Marcado como indisponível");
        tvDisp.setTextColor(getColor(t.disponivel ? R.color.verde : R.color.vermelho));

        ((MaterialButton) v.findViewById(R.id.btnPrevPublicar)).setText(btnPublicar.getText());
        v.findViewById(R.id.btnPrevEditar).setOnClickListener(b -> dialog.dismiss());
        v.findViewById(R.id.btnPrevPublicar).setOnClickListener(b -> {
            dialog.dismiss();
            publicar();
        });
        dialog.show();
    }

    private void publicar() {
        AlertDialog loading = UiUtils.carregando(this, "Publicando sua ferramenta...");
        loading.show();
        btnPublicar.setEnabled(false);

        if (latExata != null && lngExata != null) {
            salvar(latExata, lngExata, loading);
            return;
        }
        // Converte o endereço digitado em coordenadas (Geocoder)
        String cidade = UiUtils.texto(etCidade);
        String completo = UiUtils.texto(etEndereco) + ", " + UiUtils.texto(etBairro) + ", " + cidade;
        LocationHelper.coordenadasDe(this, completo, endereco -> {
            if (endereco != null) {
                usarEndereco(endereco, loading);
                return;
            }
            LocationHelper.coordenadasDe(this, cidade, enderecoCidade -> {
                if (enderecoCidade != null) {
                    usarEndereco(enderecoCidade, loading);
                } else {
                    loading.dismiss();
                    btnPublicar.setEnabled(true);
                    UiUtils.alerta(this, "Endereço não encontrado",
                            "Não conseguimos localizar esse endereço. Verifique a cidade e a internet, "
                                    + "ou toque em \"Usar minha localização atual\".");
                }
            });
        });
    }

    private void usarEndereco(Address a, AlertDialog loading) {
        if (UiUtils.texto(etBairro).isEmpty() && a.getSubLocality() != null) {
            preenchendo = true;
            etBairro.setText(a.getSubLocality());
            preenchendo = false;
        }
        salvar(a.getLatitude(), a.getLongitude(), loading);
    }

    private void salvar(double lat, double lng, AlertDialog loading) {
        Tool t = montarTool();
        // Localização pública aproximada (~1 km) — protege o endereço do proprietário
        t.latitude = LocationUtils.aproximar(lat);
        t.longitude = LocationUtils.aproximar(lng);
        t.imagens = fotosAdapter.urlsExistentes();
        List<Uri> novas = fotosAdapter.urisNovas();

        Map<String, Object> privado = new HashMap<>();
        privado.put("endereco", UiUtils.texto(etEndereco));
        privado.put("cidade", t.cidade);
        privado.put("latitudeExata", lat);
        privado.put("longitudeExata", lng);

        boolean nova = toolIdEdicao == null;
        List<String> removidas = new ArrayList<>(fotosAdapter.urlsRemovidas());
        toolRepo.salvar(this, t, novas, privado, nova, new Callback<String>() {
            @Override
            public void onSuccess(String toolId) {
                loading.dismiss();
                new StorageRepository().excluirPorUrl(removidas);
                UiUtils.toast(CadastrarFerramentaActivity.this,
                        nova ? "Ferramenta publicada com sucesso!" : "Anúncio atualizado!");
                if (nova) DetalhesFerramentaActivity.abrir(CadastrarFerramentaActivity.this, toolId);
                finish();
            }

            @Override
            public void onError(String mensagem) {
                loading.dismiss();
                btnPublicar.setEnabled(true);
                UiUtils.alerta(CadastrarFerramentaActivity.this, "Não foi possível publicar", mensagem);
            }
        });
    }
}
