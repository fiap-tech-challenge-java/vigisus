package br.com.fiap.vigisus.domain.encaminhamento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NivelPressaoSusTest {

    @Test
    void criar_quandoPercentualNegativo_lancaExcecao() {
        assertThatThrownBy(() -> new NivelPressaoSus(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Percentual de ocupação inválido");
    }

    @Test
    void criar_quandoPercentualAcimaDecem_lancaExcecao() {
        assertThatThrownBy(() -> new NivelPressaoSus(101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Percentual de ocupação inválido");
    }

    @ParameterizedTest
    @CsvSource({
            "0,  true,  false, false",
            "79, true,  false, false",
            "80, false, true,  false",
            "89, false, true,  false",
            "90, false, false, true",
            "100,false, false, true"
    })
    void classificar_retornaFaixaEsperada(int percentual,
            boolean normal, boolean alerta, boolean critico) {
        var nivel = new NivelPressaoSus(percentual);
        assertThat(nivel.isNormal()).isEqualTo(normal);
        assertThat(nivel.isAlerta()).isEqualTo(alerta);
        assertThat(nivel.isCritico()).isEqualTo(critico);
    }

    @ParameterizedTest
    @CsvSource({
            "0,  NORMAL",
            "79, NORMAL",
            "80, ALERTA",
            "89, ALERTA",
            "90, CRÍTICO",
            "100,CRÍTICO"
    })
    void descricao_retornaTextoEsperado(int percentual, String esperado) {
        assertThat(new NivelPressaoSus(percentual).descricao()).isEqualTo(esperado);
    }
}
