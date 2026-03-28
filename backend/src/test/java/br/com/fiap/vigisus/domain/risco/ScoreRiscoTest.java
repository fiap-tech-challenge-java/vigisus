package br.com.fiap.vigisus.domain.risco;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreRiscoTest {

    @Test
    void criar_quandoValorNegativo_lancaExcecao() {
        assertThatThrownBy(() -> new ScoreRisco(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Score de risco deve estar entre 0 e 100");
    }

    @Test
    void criar_quandoValorAcimaDecem_lancaExcecao() {
        assertThatThrownBy(() -> new ScoreRisco(101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Score de risco deve estar entre 0 e 100");
    }

    @ParameterizedTest
    @CsvSource({
            "0,   true,  false, false, false",
            "29,  true,  false, false, false",
            "30,  false, true,  false, false",
            "59,  false, true,  false, false",
            "60,  false, false, true,  false",
            "79,  false, false, true,  false",
            "80,  false, false, false, true",
            "100, false, false, false, true"
    })
    void classificar_retornaFaixaEsperada(int valor,
            boolean baixo, boolean moderado, boolean alto, boolean muitoAlto) {
        var score = new ScoreRisco(valor);
        assertThat(score.isBaixo()).isEqualTo(baixo);
        assertThat(score.isModerado()).isEqualTo(moderado);
        assertThat(score.isAlto()).isEqualTo(alto);
        assertThat(score.isMuitoAlto()).isEqualTo(muitoAlto);
    }

    @ParameterizedTest
    @CsvSource({
            "0,   BAIXO",
            "29,  BAIXO",
            "30,  MODERADO",
            "60,  ALTO",
            "80,  MUITO_ALTO",
            "100, MUITO_ALTO"
    })
    void classificacao_retornaTextoEsperado(int valor, String esperado) {
        assertThat(new ScoreRisco(valor).classificacao()).isEqualTo(esperado);
    }
}
