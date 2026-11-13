package com.ferramentafacil.app.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.adapter.ToolAdapter;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.repository.Callback;
import com.ferramentafacil.app.repository.ToolRepository;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.DateUtils;
import com.ferramentafacil.app.utils.LocationUtils;
import com.ferramentafacil.app.utils.MoneyUtils;
import com.ferramentafacil.app.utils.PrefsManager;
import com.ferramentafacil.app.utils.TextoUtils;
import com.ferramentafacil.app.utils.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pesquisa de ferramentas por nome/categoria com filtros
 * (categoria, preço, distância, avaliação, disponibilidade e período)
 * e ordenação (distância, preço, avaliação).
 * Ex.: buscar "furadeira" lista todas as furadeiras anunciadas.
 */
public class BuscaActivity extends AppCompatActivity {

    private EditText etBusca;
    private ChipGroup chipsOrdenacao;
    private TextView tvResumo, tvVazio;
    private ProgressBar progress;
    private ToolAdapter adapter;
    private PrefsManager prefs;
    private String uid;

    private final List<Tool> todas = new ArrayList<>();
    private final ToolRepository toolRepo = new ToolRepository();

    // Filtros atuais
    private String categoria;
    private Double precoMin, precoMax;
    private float raioKm = 0;      // 0 = sem limite
    private float notaMin = 0;     // 0 = qualquer
    private boolean somenteDisponiveis = true;
    private Long dataInicio, dataFim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busca);
        prefs = new PrefsManager(this);
        uid = FirebaseAuth.getInstance().getUid();
        categoria = getIntent().getStringExtra(Constants.EXTRA_CATEGORIA);

        etBusca = findViewById(R.id.etBusca);
        chipsOrdenacao = findViewById(R.id.chipsOrdenacao);
        tvResumo = findViewById(R.id.tvResumo);
        tvVazio = findViewById(R.id.tvVazio);
        progress = findViewById(R.id.progress);

        RecyclerView rv = findViewById(R.id.rvResultados);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ToolAdapter(false, t -> DetalhesFerramentaActivity.abrir(this, t.toolId));
        rv.setAdapter(adapter);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnFiltros).setOnClickListener(v -> abrirFiltros());
        chipsOrdenacao.setOnCheckedStateChangeListener((g, ids) -> aplicar());
        etBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { aplicar(); }
        });
        if (categoria == null) etBusca.requestFocus();

        carregar();
    }

    private void carregar() {
        progress.setVisibility(View.VISIBLE);
        toolRepo.listar(false, new Callback<List<Tool>>() {
            @Override
            public void onSuccess(List<Tool> lista) {
                progress.setVisibility(View.GONE);
                todas.clear();
                todas.addAll(lista);
                if (prefs.temLocalizacao()) {
                    for (Tool t : todas) {
                        t.distanciaKm = LocationUtils.distanciaKm(prefs.getLatitude(), prefs.getLongitude(),
                                t.latitude, t.longitude);
                    }
                }
                aplicar();
            }

            @Override
            public void onError(String mensagem) {
                progress.setVisibility(View.GONE);
                tvVazio.setVisibility(View.VISIBLE);
                tvVazio.setText(mensagem);
            }
        });
    }

    /** Aplica busca + filtros + ordenação sobre a lista carregada. */
    private void aplicar() {
        String busca = UiUtils.texto(etBusca);
        List<Tool> resultado = new ArrayList<>();
        for (Tool t : todas) {
            if (uid != null && uid.equals(t.ownerId)) continue;
            if (somenteDisponiveis && !t.disponivel) continue;
            if (categoria != null && !categoria.equals(t.categoria)) continue;
            if (!busca.isEmpty()) {
                String texto = t.nome + " " + t.categoria + " " + TextoUtils.seguro(t.marca) + " " + TextoUtils.seguro(t.modelo);
                if (!TextoUtils.contemTodas(texto, busca)) continue;
            }
            if (precoMin != null && t.valorDiaria < precoMin) continue;
            if (precoMax != null && t.valorDiaria > precoMax) continue;
            if (raioKm > 0 && (t.distanciaKm < 0 || t.distanciaKm > raioKm)) continue;
            if (notaMin > 0 && t.ownerNota < notaMin) continue;
            if (dataInicio != null && dataFim != null && t.temConflito(dataInicio, dataFim, null)) continue;
            resultado.add(t);
        }
        Collections.sort(resultado, comparador());
        adapter.setItens(resultado);

        tvResumo.setText(resultado.size() + (resultado.size() == 1 ? " resultado" : " resultados") + descreverFiltros());
        tvVazio.setVisibility(resultado.isEmpty() && progress.getVisibility() != View.VISIBLE ? View.VISIBLE : View.GONE);
    }

    private Comparator<Tool> comparador() {
        int id = chipsOrdenacao.getCheckedChipId();
        if (id == R.id.chipMenorPreco) return (a, b) -> Double.compare(a.valorDiaria, b.valorDiaria);
        if (id == R.id.chipMaiorPreco) return (a, b) -> Double.compare(b.valorDiaria, a.valorDiaria);
        if (id == R.id.chipAvaliacao) return (a, b) -> {
            int c = Double.compare(b.ownerNota, a.ownerNota);
            return c != 0 ? c : Double.compare(b.media(), a.media());
        };
        // Menor distância (sem distância conhecida vai para o fim)
        return (a, b) -> Double.compare(
                a.distanciaKm < 0 ? Double.MAX_VALUE : a.distanciaKm,
                b.distanciaKm < 0 ? Double.MAX_VALUE : b.distanciaKm);
    }

    private String descreverFiltros() {
        StringBuilder sb = new StringBuilder();
        if (categoria != null) sb.append(" · ").append(categoria);
        if (precoMin != null) sb.append(" · mín. ").append(MoneyUtils.formatar(precoMin));
        if (precoMax != null) sb.append(" · máx. ").append(MoneyUtils.formatar(precoMax));
        if (raioKm > 0) sb.append(" · até ").append(Math.round(raioKm)).append(" km");
        if (notaMin > 0) sb.append(" · nota ≥ ").append(String.format(Locale.ROOT, "%.1f", notaMin));
        if (!somenteDisponiveis) sb.append(" · inclui indisponíveis");
        if (dataInicio != null) sb.append(" · livre de ").append(DateUtils.formatarData(dataInicio))
                .append(" a ").append(DateUtils.formatarData(dataFim));
        return sb.toString();
    }

    /** Painel inferior de filtros. */
    private void abrirFiltros() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_filtros, null);
        dialog.setContentView(v);

        ChipGroup cgCategorias = v.findViewById(R.id.chipsCategorias);
        TextInputEditText etMin = v.findViewById(R.id.etPrecoMin);
        TextInputEditText etMax = v.findViewById(R.id.etPrecoMax);
        TextView tvRaio = v.findViewById(R.id.tvRaio);
        Slider sliderRaio = v.findViewById(R.id.sliderRaio);
        TextView tvNota = v.findViewById(R.id.tvNotaMin);
        Slider sliderNota = v.findViewById(R.id.sliderNota);
        MaterialSwitch swDisp = v.findViewById(R.id.swDisponiveis);
        MaterialButton btnPeriodo = v.findViewById(R.id.btnPeriodo);

        // Categorias
        for (String cat : Constants.CATEGORIAS) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filtro, cgCategorias, false);
            chip.setText(cat);
            chip.setChecked(cat.equals(categoria));
            cgCategorias.addView(chip);
        }

        if (precoMin != null) etMin.setText(MoneyUtils.paraCampo(precoMin));
        if (precoMax != null) etMax.setText(MoneyUtils.paraCampo(precoMax));

        sliderRaio.setValue(raioKm);
        tvRaio.setText(textoRaio(raioKm));
        sliderRaio.addOnChangeListener((s, valor, u) -> tvRaio.setText(textoRaio(valor)));

        sliderNota.setValue(notaMin);
        tvNota.setText(textoNota(notaMin));
        sliderNota.addOnChangeListener((s, valor, u) -> tvNota.setText(textoNota(valor)));

        swDisp.setChecked(somenteDisponiveis);

        final Long[] periodo = {dataInicio, dataFim};
        btnPeriodo.setText(textoPeriodo(periodo[0], periodo[1]));
        btnPeriodo.setOnClickListener(b -> {
            CalendarConstraints restricoes = new CalendarConstraints.Builder()
                    .setStart(DateUtils.hojeUtc())
                    .setValidator(DateValidatorPointForward.from(DateUtils.hojeUtc()))
                    .build();
            MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("Período desejado")
                    .setCalendarConstraints(restricoes)
                    .build();
            picker.addOnPositiveButtonClickListener(sel -> {
                periodo[0] = sel.first;
                periodo[1] = sel.second;
                btnPeriodo.setText(textoPeriodo(periodo[0], periodo[1]));
            });
            picker.show(getSupportFragmentManager(), "periodo_busca");
        });

        v.findViewById(R.id.btnLimpar).setOnClickListener(b -> {
            categoria = null;
            precoMin = precoMax = null;
            raioKm = 0;
            notaMin = 0;
            somenteDisponiveis = true;
            dataInicio = dataFim = null;
            dialog.dismiss();
            aplicar();
        });

        v.findViewById(R.id.btnAplicar).setOnClickListener(b -> {
            int checado = cgCategorias.getCheckedChipId();
            categoria = null;
            if (checado != View.NO_ID) {
                Chip c = cgCategorias.findViewById(checado);
                categoria = c.getText().toString();
            }
            precoMin = MoneyUtils.ler(UiUtils.texto(etMin));
            precoMax = MoneyUtils.ler(UiUtils.texto(etMax));
            raioKm = sliderRaio.getValue();
            notaMin = sliderNota.getValue();
            somenteDisponiveis = swDisp.isChecked();
            dataInicio = periodo[0];
            dataFim = periodo[1];
            if (raioKm > 0 && !prefs.temLocalizacao()) {
                UiUtils.toast(this, "Defina sua localização na tela inicial para filtrar por distância.");
            }
            dialog.dismiss();
            aplicar();
        });

        dialog.show();
    }

    private static String textoRaio(float v) {
        return v <= 0 ? "Distância máxima: sem limite" : "Distância máxima: " + Math.round(v) + " km";
    }

    private static String textoNota(float v) {
        return v <= 0 ? "Avaliação mínima do proprietário: qualquer"
                : "Avaliação mínima do proprietário: " + String.format(Locale.ROOT, "%.1f", v) + " ★";
    }

    private static String textoPeriodo(Long ini, Long fim) {
        if (ini == null || fim == null) return "Qualquer data";
        return DateUtils.formatarData(ini) + " → " + DateUtils.formatarData(fim);
    }
}
