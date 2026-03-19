package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Municipio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrevisaoRiscoServiceTest {

    @Mock
    private MunicipioService municipioService;

    @Mock
    private ClimaService climaService;

    private PrevisaoRiscoService service;

    private static final Municipio LAVRAS = Municipio.builder()
            .coIbge("3131307")
            .noMunicipio("Lavras")
            .sgUf("MG")
            .nuLatitude(-21.245)
            .nuLongitude(-44.999)
            .build();

    @BeforeEach
    void setUp() {
        service = new PrevisaoRiscoService(municipioService, climaService);
        when(municipioService.buscarPorCoIbge("3131307")).thenReturn(LAVRAS);
    }

    // ── testScoreAltoComTemperaturaEChuva ─────────────────────────────────────

    @Test
    void testScoreAltoComTemperaturaEChuva() {
        // temp atual >= 28 → +2
        // temp média 14 dias >= 28 → +2
        // chuva total 14 dias: 7mm * 14 = 98mm (between 50 and 100) → +1
        // umidade < 80 → 0
        // prob chuva < 60 → 0
        // total score = 5 → ALTO
        ClimaAtualDTO clima = ClimaAtualDTO.builder()
                .temperatura(30.0)
                .umidade(70)
                .precipitacao(0.0)
                .build();
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(clima);

        List<PrevisaoDiariaDTO> previsao = buildPrevisao(14, 32.0, 7.0, 40);
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(previsao);

        PrevisaoRiscoResponse response = service.calcularRisco("3131307");

        assertThat(response.getScore()).isGreaterThanOrEqualTo(4);
        assertThat(response.getClassificacao()).isEqualTo("ALTO");
    }

    // ── testScoreBaixoSemChuvaETemperaturaBaixa ───────────────────────────────

    @Test
    void testScoreBaixoSemChuvaETemperaturaBaixa() {
        // temp atual < 25 → 0; umidade < 80 → 0; chuva total = 0 → 0 → score=0 → BAIXO
        ClimaAtualDTO clima = ClimaAtualDTO.builder()
                .temperatura(20.0)
                .umidade(50)
                .precipitacao(0.0)
                .build();
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(clima);

        List<PrevisaoDiariaDTO> previsao = buildPrevisao(14, 22.0, 0.0, 10);
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(previsao);

        PrevisaoRiscoResponse response = service.calcularRisco("3131307");

        assertThat(response.getScore()).isLessThanOrEqualTo(1);
        assertThat(response.getClassificacao()).isEqualTo("BAIXO");
    }

    // ── testClassificacaoMuitoAltoQuandoScore6ouMais ──────────────────────────

    @Test
    void testClassificacaoMuitoAltoQuandoScore6ouMais() {
        // temp atual >= 28 → +2; umidade >= 80 → +1
        // temp média 14 dias >= 28 → +2; chuva total >= 100mm → +2 → score=7 → MUITO_ALTO
        ClimaAtualDTO clima = ClimaAtualDTO.builder()
                .temperatura(32.0)
                .umidade(85)
                .precipitacao(5.0)
                .build();
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(clima);

        List<PrevisaoDiariaDTO> previsao = buildPrevisao(14, 30.0, 10.0, 70);
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(previsao);

        PrevisaoRiscoResponse response = service.calcularRisco("3131307");

        assertThat(response.getScore()).isGreaterThanOrEqualTo(6);
        assertThat(response.getClassificacao()).isEqualTo("MUITO_ALTO");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<PrevisaoDiariaDTO> buildPrevisao(int days, double tempMax, double precip, int probChuva) {
        return IntStream.range(0, days)
                .mapToObj(i -> PrevisaoDiariaDTO.builder()
                        .data(LocalDate.of(2024, 1, 1).plusDays(i).toString())
                        .temperaturaMaxima(tempMax)
                        .precipitacaoTotal(precip)
                        .probabilidadeChuva(probChuva)
                        .build())
                .toList();
    }
}
