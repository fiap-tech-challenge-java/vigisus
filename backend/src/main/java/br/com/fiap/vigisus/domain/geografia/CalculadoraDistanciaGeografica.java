package br.com.fiap.vigisus.domain.geografia;

import org.springframework.stereotype.Component;

@Component
public class CalculadoraDistanciaGeografica {

    public double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double raioTerraKm = 6371;
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(deltaLon / 2), 2);
        return raioTerraKm * 2 * Math.asin(Math.sqrt(a));
    }
}
