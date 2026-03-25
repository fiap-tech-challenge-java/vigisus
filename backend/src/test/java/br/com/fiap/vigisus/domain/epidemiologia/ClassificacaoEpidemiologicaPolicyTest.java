package br.com.fiap.vigisus.domain.epidemiologia;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificacaoEpidemiologicaPolicyTest {

    private final ClassificacaoEpidemiologicaPolicy policy = new ClassificacaoEpidemiologicaPolicy();

    @ParameterizedTest
    @CsvSource({
            "0.0,BAIXO",
            "49.99,BAIXO",
            "50.0,MODERADO",
            "99.99,MODERADO",
            "100.0,ALTO",
            "300.0,ALTO",
            "300.01,EPIDEMIA",
            "500.0,EPIDEMIA"
    })
    void classificaNosLimiaresEsperados(double incidencia, String esperado) {
        assertThat(policy.classificar(incidencia)).isEqualTo(esperado);
    }
}
