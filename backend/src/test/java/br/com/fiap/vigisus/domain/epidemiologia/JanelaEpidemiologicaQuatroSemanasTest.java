package br.com.fiap.vigisus.domain.epidemiologia;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class JanelaEpidemiologicaQuatroSemanasTest {

    @Test
    void daData_quandoSemanaRegular_contemSomenteSemanasDoAnoAtual() {
        JanelaEpidemiologicaQuatroSemanas janela =
                JanelaEpidemiologicaQuatroSemanas.daData(LocalDate.of(2026, 3, 25));

        assertThat(janela.anoAtual()).isEqualTo(2026);
        assertThat(janela.anoAnterior()).isEqualTo(2025);
        assertThat(janela.semanasAnoAtual()).hasSize(4);
        assertThat(janela.semanasAnoAnterior()).isEmpty();
        assertThat(janela.semanasOrdenadas()).hasSize(4);
        assertThat(janela.semanasOrdenadas()).extracting(SemanaEpidemiologica::ano).containsOnly(2026);
    }

    @Test
    void daData_quandoInicioDeAno_distribuiSemanasEntreAnos() {
        JanelaEpidemiologicaQuatroSemanas janela =
                JanelaEpidemiologicaQuatroSemanas.daData(LocalDate.of(2024, 1, 2));

        assertThat(janela.anoAtual()).isEqualTo(2024);
        assertThat(janela.anoAnterior()).isEqualTo(2023);
        assertThat(janela.semanasAnoAtual()).containsExactly(1);
        assertThat(janela.semanasAnoAnterior()).containsExactly(50, 51, 52);
        assertThat(janela.semanasOrdenadas()).extracting(SemanaEpidemiologica::ano)
                .containsExactly(2023, 2023, 2023, 2024);
    }

    @Test
    void semanasMesmoPeriodoAnoAnterior_retornaSemanasDoAnoAtualQuandoExistirem() {
        JanelaEpidemiologicaQuatroSemanas janela =
                JanelaEpidemiologicaQuatroSemanas.daData(LocalDate.of(2026, 3, 25));

        assertThat(janela.semanasMesmoPeriodoAnoAnterior()).isEqualTo(janela.semanasAnoAtual());
    }
}
