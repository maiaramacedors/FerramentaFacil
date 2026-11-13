package com.ferramentafacil.app.utils;

import java.util.Locale;

/** Cálculos de distância e aproximação de coordenadas. */
public final class LocationUtils {

    private static final double RAIO_TERRA_KM = 6371.0;

    private LocationUtils() { }

    /** Distância em km entre dois pontos (fórmula de Haversine). */
    public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAIO_TERRA_KM * c;
    }

    /**
     * Arredonda a coordenada para proteger o endereço do proprietário.
     * Com 2 casas decimais a precisão fica em torno de 1 km.
     */
    public static double aproximar(double coordenada) {
        double fator = Math.pow(10, Constants.CASAS_APROXIMACAO);
        return Math.round(coordenada * fator) / fator;
    }

    public static String formatarDistancia(double km) {
        if (km < 0) return "Distância indisponível";
        if (km < 1) return "menos de 1 km";
        if (km < 10) return String.format(new Locale("pt", "BR"), "%.1f km", km);
        return Math.round(km) + " km";
    }
}
