package br.com.fiap.vigisus.domain.epidemiologia;

import org.springframework.stereotype.Component;

@Component
public class ClassificacaoEpidemiologicaPolicy {

    private static final double THRESHOLD_MODERADO = 50.0;
    private static final double THRESHOLD_ALTO = 100.0;
    private static final double THRESHOLD_EPIDEMIA = 300.0;

    public String classificar(double incidencia) {
        if (incidencia < THRESHOLD_MODERADO) {
            return "BAIXO";
        }
        if (incidencia < THRESHOLD_ALTO) {
            return "MODERADO";
        }
        if (incidencia <= THRESHOLD_EPIDEMIA) {
            return "ALTO";
        }
        return "EPIDEMIA";
    }
}
