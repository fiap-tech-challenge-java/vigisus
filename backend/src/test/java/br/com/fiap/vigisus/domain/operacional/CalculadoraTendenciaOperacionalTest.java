package br.com.fiap.vigisus.domain.operacional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraTendenciaOperacionalTest {

    private final CalculadoraTendenciaOperacional calculadora = new CalculadoraTendenciaOperacional();

    @ParameterizedTest
    @CsvSource({
            "5,0,CRESCENTE",
            "0,0,ESTAVEL",
            "13,10,CRESCENTE",
            "7,10,DECRESCENTE",
            "11,10,ESTAVEL"
    })
    void calcular_retornaTendenciaEsperada(long casosAtual, long casosSemana3Atras, String esperado) {
        assertThat(calculadora.calcular(casosAtual, casosSemana3Atras)).isEqualTo(esperado);
    }
}
