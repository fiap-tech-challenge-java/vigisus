package br.com.fiap.vigisus.domain.risco;

import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;
import br.com.fiap.vigisus.dto.RiscoDiarioDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraRiscoClimaticoTest {

    private final CalculadoraRiscoClimatico calculadora = new CalculadoraRiscoClimatico();

    @Test
    void extraiMetricasECalculaScoreAgregado() {
        ClimaAtualDTO climaAtual = ClimaAtualDTO.builder()
                .temperatura(29.0)
                .umidade(85)
                .build();

        MetricasRiscoClimatico metricas = calculadora.extrairMetricas(climaAtual, previsaoConstante(16, 30.0, 8.0, 70));

        assertThat(metricas.temperaturaAtual()).isEqualTo(29.0);
        assertThat(metricas.umidadeAtual()).isEqualTo(85);
        assertThat(metricas.temperaturaMedia14Dias()).isEqualTo(30.0);
        assertThat(metricas.chuvaTotal14Dias()).isEqualTo(112.0);
        assertThat(metricas.probabilidadeMediaChuva14Dias()).isEqualTo(70.0);
        assertThat(calculadora.calcularScore(metricas)).isEqualTo(8);
    }

    @Test
    void calculaRiscoDiarioComPolicyEDataResolver() {
        List<RiscoDiarioDTO> riscoDiario = calculadora.calcularRiscoDiario(
                previsaoConstante(16, 28.0, 10.0, 60),
                new ClassificacaoRiscoMunicipioPolicy(),
                (indice, dia) -> LocalDate.of(2024, 1, 1).plusDays(indice).toString()
        );

        assertThat(riscoDiario).hasSize(14);
        assertThat(riscoDiario.get(0).getScoreDia()).isEqualTo(4);
        assertThat(riscoDiario.get(0).getClassificacao()).isEqualTo("ALTO");
        assertThat(riscoDiario.get(13).getData()).isEqualTo("2024-01-14");
    }

    @Test
    void retornaListaVaziaQuandoNaoHaPrevisao() {
        List<RiscoDiarioDTO> riscoDiario = calculadora.calcularRiscoDiario(
                null,
                new ClassificacaoRiscoAgregadoPolicy(),
                (indice, dia) -> "2024-01-01"
        );

        assertThat(riscoDiario).isEmpty();
    }

    private List<PrevisaoDiariaDTO> previsaoConstante(int dias, double temperatura, double chuva, int probabilidade) {
        return IntStream.range(0, dias)
                .mapToObj(indice -> PrevisaoDiariaDTO.builder()
                        .data(LocalDate.of(2024, 1, 1).plusDays(indice).toString())
                        .temperaturaMaxima(temperatura)
                        .precipitacaoTotal(chuva)
                        .probabilidadeChuva(probabilidade)
                        .build())
                .toList();
    }
}
