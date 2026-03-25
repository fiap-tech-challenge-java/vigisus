package br.com.fiap.vigisus.domain.geografia;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CalculadoraDistanciaGeograficaTest {

    private final CalculadoraDistanciaGeografica calculadora = new CalculadoraDistanciaGeografica();

    @Test
    void haversine_quandoMesmoPonto_retornaZero() {
        assertThat(calculadora.haversine(-23.5505, -46.6333, -23.5505, -46.6333)).isEqualTo(0.0);
    }

    @Test
    void haversine_quandoSaoPauloERio_retornaDistanciaAproximada() {
        assertThat(calculadora.haversine(-23.5505, -46.6333, -22.9068, -43.1729))
                .isCloseTo(357.0, within(5.0));
    }
}
