package br.com.fiap.vigisus.domain.risco;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificacaoRiscoPolicyTest {

    private final ClassificacaoRiscoMunicipioPolicy classificacaoMunicipio = new ClassificacaoRiscoMunicipioPolicy();
    private final ClassificacaoRiscoAgregadoPolicy classificacaoAgregado = new ClassificacaoRiscoAgregadoPolicy();

    @ParameterizedTest
    @CsvSource({
            "0,BAIXO",
            "1,BAIXO",
            "2,MODERADO",
            "3,MODERADO",
            "4,ALTO",
            "5,ALTO",
            "6,MUITO_ALTO",
            "10,MUITO_ALTO"
    })
    void classificaRiscoMunicipalNosLimiaresEsperados(int score, String esperado) {
        assertThat(classificacaoMunicipio.classificar(score)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "0,BAIXO",
            "1,BAIXO",
            "2,MODERADO",
            "3,MODERADO",
            "4,ALTO",
            "5,ALTO",
            "6,EPIDEMIA",
            "10,EPIDEMIA"
    })
    void classificaRiscoAgregadoNosLimiaresEsperados(int score, String esperado) {
        assertThat(classificacaoAgregado.classificar(score)).isEqualTo(esperado);
    }
}
