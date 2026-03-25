package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.domain.geografia.CatalogoGeograficoBrasil;
import br.com.fiap.vigisus.domain.risco.CalculadoraRiscoClimatico;
import br.com.fiap.vigisus.domain.risco.ClassificacaoRiscoMunicipioPolicy;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
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
        service = new PrevisaoRiscoService(
                municipioService,
                climaService,
                new CatalogoGeograficoBrasil(),
                new CalculadoraRiscoClimatico(),
                new ClassificacaoRiscoMunicipioPolicy()
        );
        lenient().when(municipioService.buscarPorCoIbge("3131307")).thenReturn(LAVRAS);
    }

    @Test
    void testScoreAltoComTemperaturaEChuva() {
        ClimaAtualDTO clima = ClimaAtualDTO.builder()
                .temperatura(30.0)
                .umidade(70)
                .precipitacao(0.0)
                .build();
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(clima);
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(buildPrevisao(14, 32.0, 7.0, 40));

        PrevisaoRiscoResponse response = service.calcularRisco("3131307");

        assertThat(response.getScore()).isGreaterThanOrEqualTo(4);
        assertThat(response.getClassificacao()).isEqualTo("ALTO");
    }

    @Test
    void testScoreBaixoSemChuvaETemperaturaBaixa() {
        ClimaAtualDTO clima = ClimaAtualDTO.builder()
                .temperatura(20.0)
                .umidade(50)
                .precipitacao(0.0)
                .build();
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(clima);
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(buildPrevisao(14, 22.0, 0.0, 10));

        PrevisaoRiscoResponse response = service.calcularRisco("3131307");

        assertThat(response.getScore()).isLessThanOrEqualTo(1);
        assertThat(response.getClassificacao()).isEqualTo("BAIXO");
    }

    @Test
    void testClassificacaoMuitoAltoQuandoScore6ouMais() {
        ClimaAtualDTO clima = ClimaAtualDTO.builder()
                .temperatura(32.0)
                .umidade(85)
                .precipitacao(5.0)
                .build();
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(clima);
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(buildPrevisao(14, 30.0, 10.0, 70));

        PrevisaoRiscoResponse response = service.calcularRisco("3131307");

        assertThat(response.getScore()).isGreaterThanOrEqualTo(6);
        assertThat(response.getClassificacao()).isEqualTo("MUITO_ALTO");
    }

    @Test
    void calcularRisco_quandoCoordenadasZero_usaCentroideDoEstado() {
        Municipio municipioSemCoordenada = Municipio.builder()
                .coIbge("3550308")
                .noMunicipio("Sao Paulo")
                .sgUf("SP")
                .nuLatitude(0.0)
                .nuLongitude(0.0)
                .build();
        when(municipioService.buscarPorCoIbge("3550308")).thenReturn(municipioSemCoordenada);
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(ClimaAtualDTO.builder().temperatura(20.0).umidade(50).build());
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(buildPrevisao(16, 22.0, 0.0, 10));

        PrevisaoRiscoResponse response = service.calcularRisco("3550308");

        assertThat(response.getMunicipio()).isEqualTo("Sao Paulo");
        verify(climaService).buscarClimaAtual(eq(-22.1875), eq(-48.7966));
        verify(climaService).buscarPrevisao16Dias(eq(-22.1875), eq(-48.7966));
    }

    @Test
    void calcularRisco_quandoUfNula_usaCentroidePadrao() {
        Municipio municipioSemUf = Municipio.builder()
                .coIbge("0000001")
                .noMunicipio("Sem UF")
                .sgUf(null)
                .nuLatitude(null)
                .nuLongitude(null)
                .build();
        when(municipioService.buscarPorCoIbge("0000001")).thenReturn(municipioSemUf);
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(ClimaAtualDTO.builder().temperatura(24.0).umidade(79).build());
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(buildPrevisao(16, 24.0, 0.0, 0));

        service.calcularRisco("0000001");

        verify(climaService).buscarClimaAtual(eq(-15.7801), eq(-47.9292));
        verify(climaService).buscarPrevisao16Dias(eq(-15.7801), eq(-47.9292));
    }

    @Test
    void calcularRisco_quandoUfDesconhecida_usaCentroidePadraoDoSwitch() {
        Municipio municipioSemMapa = Municipio.builder()
                .coIbge("0000002")
                .noMunicipio("Cidade X")
                .sgUf("XX")
                .nuLatitude(null)
                .nuLongitude(null)
                .build();
        when(municipioService.buscarPorCoIbge("0000002")).thenReturn(municipioSemMapa);
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(ClimaAtualDTO.builder().temperatura(24.0).umidade(79).build());
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(buildPrevisao(16, 24.0, 0.0, 0));

        service.calcularRisco("0000002");

        verify(climaService).buscarClimaAtual(eq(-15.7801), eq(-47.9292));
        verify(climaService).buscarPrevisao16Dias(eq(-15.7801), eq(-47.9292));
    }

    @Test
    void calcularRisco_classificaModeradoComLimiarIntermediario() {
        ClimaAtualDTO clima = ClimaAtualDTO.builder()
                .temperatura(26.0)
                .umidade(80)
                .build();
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(clima);
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(buildPrevisao(14, 24.0, 0.0, 70));

        PrevisaoRiscoResponse response = service.calcularRisco("3131307");

        assertThat(response.getScore()).isEqualTo(3);
        assertThat(response.getClassificacao()).isEqualTo("MODERADO");
        assertThat(response.getFatores()).hasSize(3);
    }

    @Test
    void calcularRisco_trataValoresNulosEDelimitaRiscoA14Dias() {
        ClimaAtualDTO clima = ClimaAtualDTO.builder()
                .temperatura(null)
                .umidade(null)
                .build();
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(clima);
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(previsaoComDiasVariados());

        PrevisaoRiscoResponse response = service.calcularRisco("3131307");

        assertThat(response.getScore()).isZero();
        assertThat(response.getClassificacao()).isEqualTo("BAIXO");
        assertThat(response.getRisco14Dias()).hasSize(14);
        assertThat(response.getRisco14Dias().get(0).getClassificacao()).isEqualTo("BAIXO");
        assertThat(response.getRisco14Dias().get(1).getClassificacao()).isEqualTo("MODERADO");
        assertThat(response.getRisco14Dias().get(2).getClassificacao()).isEqualTo("ALTO");
        assertThat(response.getRisco14Dias().get(13).getData()).isEqualTo("2024-01-14");
    }

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

    private List<PrevisaoDiariaDTO> previsaoComDiasVariados() {
        List<PrevisaoDiariaDTO> previsao = new ArrayList<>();
        previsao.add(PrevisaoDiariaDTO.builder().data("2024-01-01").temperaturaMaxima(null).precipitacaoTotal(null).probabilidadeChuva(null).build());
        previsao.add(PrevisaoDiariaDTO.builder().data("2024-01-02").temperaturaMaxima(26.0).precipitacaoTotal(10.0).probabilidadeChuva(0).build());
        previsao.add(PrevisaoDiariaDTO.builder().data("2024-01-03").temperaturaMaxima(28.0).precipitacaoTotal(10.0).probabilidadeChuva(60).build());
        previsao.add(PrevisaoDiariaDTO.builder().data("2024-01-04").temperaturaMaxima(22.0).precipitacaoTotal(0.0).probabilidadeChuva(0).build());
        previsao.addAll(IntStream.range(4, 16)
                .mapToObj(i -> PrevisaoDiariaDTO.builder()
                        .data(LocalDate.of(2024, 1, 1).plusDays(i).toString())
                        .temperaturaMaxima(null)
                        .precipitacaoTotal(null)
                        .probabilidadeChuva(null)
                        .build())
                .toList());
        return previsao;
    }
}
