package br.com.fiap.vigisus.domain.operacional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraNivelAtencaoOperacionalTest {

    private final CalculadoraNivelAtencaoOperacional calculadora = new CalculadoraNivelAtencaoOperacional();

    @ParameterizedTest
    @CsvSource({
            "7,BAIXO,ESTAVEL,BAIXO,NORMAL",
            "5,EPIDEMIA,ESTAVEL,BAIXO,ELEVADO",
            "10,EPIDEMIA,CRESCENTE,MUITO_ALTO,CRITICO",
            "2,MODERADO,ESTAVEL,ALTO,ELEVADO",
            "0,BAIXO,ESTAVEL,BAIXO,NORMAL",
            "5,ALTO,CRESCENTE,BAIXO,CRITICO"
    })
    void calcular_retornaNivelEsperado(int suspeitasDia, String classificacao, String tendencia,
                                       String classificacaoRiscoClimatico, String esperado) {
        assertThat(calculadora.calcular(
                suspeitasDia,
                classificacao,
                tendencia,
                classificacaoRiscoClimatico
        )).isEqualTo(esperado);
    }
}
