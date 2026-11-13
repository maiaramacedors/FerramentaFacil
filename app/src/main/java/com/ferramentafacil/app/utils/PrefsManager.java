package com.ferramentafacil.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferências locais (SharedPreferences): localização de busca escolhida e raio de distância.
 * A localização fica apenas no aparelho — não é enviada ao servidor.
 */
public class PrefsManager {

    private static final String ARQUIVO = "ferramentafacil_prefs";
    private static final String K_LAT = "lat";
    private static final String K_LNG = "lng";
    private static final String K_CIDADE = "cidade";
    private static final String K_RAIO = "raio_km";

    private final SharedPreferences sp;

    public PrefsManager(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public boolean temLocalizacao() {
        return sp.contains(K_LAT) && sp.contains(K_LNG);
    }

    public double getLatitude() {
        return Double.longBitsToDouble(sp.getLong(K_LAT, 0));
    }

    public double getLongitude() {
        return Double.longBitsToDouble(sp.getLong(K_LNG, 0));
    }

    public String getCidade() {
        return sp.getString(K_CIDADE, "");
    }

    public void salvarLocalizacao(double lat, double lng, String cidade) {
        sp.edit()
                .putLong(K_LAT, Double.doubleToRawLongBits(lat))
                .putLong(K_LNG, Double.doubleToRawLongBits(lng))
                .putString(K_CIDADE, cidade)
                .apply();
    }

    public float getRaioKm() {
        return sp.getFloat(K_RAIO, Constants.RAIO_PADRAO_KM);
    }

    public void salvarRaioKm(float raio) {
        sp.edit().putFloat(K_RAIO, raio).apply();
    }

    public void limpar() {
        sp.edit().clear().apply();
    }
}
