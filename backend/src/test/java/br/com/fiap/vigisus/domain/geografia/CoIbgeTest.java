package br.com.fiap.vigisus.domain.geografia;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoIbgeTest {

    @Test
    void of_quandoValorValido_normalizaEspacos() {
        CoIbge coIbge = CoIbge.of(" 3131307 ");

        assertThat(coIbge.value()).isEqualTo("3131307");
        assertThat(coIbge).hasToString("3131307");
    }

    @Test
    void of_quandoVazio_lancaErro() {
        assertThatThrownBy(() -> CoIbge.of("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coIbge");
    }
}
