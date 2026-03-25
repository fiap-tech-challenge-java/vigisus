package br.com.fiap.vigisus.domain.epidemiologia;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemanaEpidemiologicaTest {

    @Test
    void criar_quandoValoresValidos_retornaRecord() {
        SemanaEpidemiologica semana = new SemanaEpidemiologica(2026, 12);

        assertThat(semana.ano()).isEqualTo(2026);
        assertThat(semana.numero()).isEqualTo(12);
    }

    @Test
    void criar_quandoAnoInvalido_lancaExcecao() {
        assertThatThrownBy(() -> new SemanaEpidemiologica(0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ano epidemiologico invalido");
    }

    @Test
    void criar_quandoSemanaInvalida_lancaExcecao() {
        assertThatThrownBy(() -> new SemanaEpidemiologica(2026, 54))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Semana epidemiologica invalida");
    }
}
