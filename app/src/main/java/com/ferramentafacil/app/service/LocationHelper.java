package com.ferramentafacil.app.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serviço de localização:
 *  - obtém a posição atual pelo Fused Location Provider (Google Play Services);
 *  - converte coordenadas ↔ endereço com o Geocoder do Android (em segundo plano).
 */
public final class LocationHelper {

    public interface ResultadoLocalizacao {
        void onLocalizacao(double latitude, double longitude);

        void onErro(String mensagem);
    }

    public interface ResultadoEndereco {
        void onResultado(@Nullable Address endereco);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Locale BR = new Locale("pt", "BR");

    private LocationHelper() { }

    public static boolean temPermissao(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static final String[] PERMISSOES = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @SuppressLint("MissingPermission")
    public static void obterLocalizacaoAtual(Context c, ResultadoLocalizacao cb) {
        if (!temPermissao(c)) {
            cb.onErro("Permissão de localização não concedida.");
            return;
        }
        FusedLocationProviderClient cliente = LocationServices.getFusedLocationProviderClient(c);
        cliente.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        cb.onLocalizacao(loc.getLatitude(), loc.getLongitude());
                    } else {
                        // Tenta a última localização conhecida
                        cliente.getLastLocation().addOnSuccessListener(ultima -> {
                            if (ultima != null) cb.onLocalizacao(ultima.getLatitude(), ultima.getLongitude());
                            else cb.onErro("Não foi possível obter sua localização. Verifique se o GPS está ligado.");
                        }).addOnFailureListener(e -> cb.onErro("Não foi possível obter sua localização."));
                    }
                })
                .addOnFailureListener(e -> cb.onErro("Não foi possível obter sua localização. Verifique se o GPS está ligado."));
    }

    /** Coordenadas → endereço. */
    @SuppressWarnings("deprecation")
    public static void enderecoDe(Context c, double lat, double lng, ResultadoEndereco cb) {
        Context app = c.getApplicationContext();
        EXECUTOR.execute(() -> {
            Address resultado = null;
            try {
                if (Geocoder.isPresent()) {
                    List<Address> lista = new Geocoder(app, BR).getFromLocation(lat, lng, 1);
                    if (lista != null && !lista.isEmpty()) resultado = lista.get(0);
                }
            } catch (Exception ignorado) {
                // Sem internet ou serviço indisponível
            }
            Address r = resultado;
            MAIN.post(() -> cb.onResultado(r));
        });
    }

    /** Texto (endereço ou cidade) → coordenadas. */
    @SuppressWarnings("deprecation")
    public static void coordenadasDe(Context c, String texto, ResultadoEndereco cb) {
        Context app = c.getApplicationContext();
        EXECUTOR.execute(() -> {
            Address resultado = null;
            try {
                if (Geocoder.isPresent()) {
                    List<Address> lista = new Geocoder(app, BR).getFromLocationName(texto + ", Brasil", 1);
                    if (lista != null && !lista.isEmpty()) resultado = lista.get(0);
                }
            } catch (Exception ignorado) {
                // Sem internet ou serviço indisponível
            }
            Address r = resultado;
            MAIN.post(() -> cb.onResultado(r));
        });
    }

    /** Nome da cidade de um endereço (o Geocoder às vezes devolve a cidade em subAdminArea). */
    @Nullable
    public static String cidade(@Nullable Address a) {
        if (a == null) return null;
        if (a.getLocality() != null) return a.getLocality();
        return a.getSubAdminArea();
    }

    /** "Indaiatuba - SP" */
    public static String descreverCidade(@Nullable Address a) {
        if (a == null) return "Local selecionado";
        String cidade = cidade(a);
        String uf = sigla(a.getAdminArea());
        if (cidade == null) return uf != null ? uf : "Local selecionado";
        return uf != null ? cidade + " - " + uf : cidade;
    }

    private static final Map<String, String> UFS = new HashMap<>();

    static {
        String[][] estados = {
                {"Acre", "AC"}, {"Alagoas", "AL"}, {"Amapá", "AP"}, {"Amazonas", "AM"}, {"Bahia", "BA"},
                {"Ceará", "CE"}, {"Distrito Federal", "DF"}, {"Espírito Santo", "ES"}, {"Goiás", "GO"},
                {"Maranhão", "MA"}, {"Mato Grosso", "MT"}, {"Mato Grosso do Sul", "MS"}, {"Minas Gerais", "MG"},
                {"Pará", "PA"}, {"Paraíba", "PB"}, {"Paraná", "PR"}, {"Pernambuco", "PE"}, {"Piauí", "PI"},
                {"Rio de Janeiro", "RJ"}, {"Rio Grande do Norte", "RN"}, {"Rio Grande do Sul", "RS"},
                {"Rondônia", "RO"}, {"Roraima", "RR"}, {"Santa Catarina", "SC"}, {"São Paulo", "SP"},
                {"Sergipe", "SE"}, {"Tocantins", "TO"}
        };
        for (String[] e : estados) UFS.put(e[0], e[1]);
    }

    @Nullable
    private static String sigla(@Nullable String estado) {
        if (estado == null) return null;
        if (estado.length() == 2) return estado.toUpperCase(BR);
        String s = UFS.get(estado);
        return s != null ? s : estado;
    }
}
