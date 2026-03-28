package br.com.fiap.vigisus.domain.epidemiologia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidenciaPor100kHabTest {

    @Test
    void criar_quandoValorNegativo_lancaExcecao() {
        assertThatThrownBy(() -> new IncidenciaPor100kHab(-1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incidência não pode ser negativa");
    }

    @Test
    void criar_quandoValorZero_naoLancaExcecao() {
        var incidencia = new IncidenciaPor100kHab(0.0);
        assertThat(incidencia.valor()).isEqualTo(0.0);
    }

    @ParameterizedTest
    @CsvSource({
            "0.0,   true,  false, false, false",
            "99.9,  true,  false, false, false",
            "100.0, false, true,  false, false",
            "300.0, false, false, true,  false",
            "1000.0,false, false, false, true"
    })
    void classificar_retornaFaixaEsperada(double valor,
            boolean baixa, boolean media, boolean alta, boolean muitoAlta) {
        var incidencia = new IncidenciaPor100kHab(valor);
        assertThat(incidencia.isBaixa()).isEqualTo(baixa);
        assertThat(incidencia.isMedia()).isEqualTo(media);
        assertThat(incidencia.isAlta()).isEqualTo(alta);
        assertThat(incidencia.isMuitoAlta()).isEqualTo(muitoAlta);
    }

    @Test
    void calcular_quandoPopulacaoNormal_retornaIncidenciaCorreta() {
        var incidencia = IncidenciaPor100kHab.calcular(150, 100_000);
        assertThat(incidencia.valor()).isEqualTo(150.0);
    }

    @Test
    void calcular_quandoPopulacaoZero_retornaZero() {
        var incidencia = IncidenciaPor100kHab.calcular(100, 0);
        assertThat(incidencia.valor()).isEqualTo(0.0);
    }

    @Test
    void toString_retornaFormatoEsperado() {
        var incidencia = new IncidenciaPor100kHab(250.5);
        assertThat(incidencia.toString()).isEqualTo("250.5/100k hab");
    }
}
