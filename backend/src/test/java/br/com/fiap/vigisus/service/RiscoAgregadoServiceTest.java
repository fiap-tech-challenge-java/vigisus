package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.EstabelecimentoRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiscoAgregadoServiceTest {

    @Mock
    private MunicipioRepository municipioRepository;

    @Mock
    private CasoDengueRepository casoDengueRepository;

    @Mock
    private EstabelecimentoRepository estabelecimentoRepository;

    @Mock
    private ClimaService climaService;

    @Mock
    private PrevisaoRiscoService previsaoRiscoService;

    private RiscoAgregadoService service;

    @BeforeEach
    void setUp() {
        service = new RiscoAgregadoService(
                municipioRepository, casoDengueRepository, estabelecimentoRepository, climaService, previsaoRiscoService);
    }

    @Test
    void calcularRiscoBrasil_retornaRespostaComIncidenciaERiscoDiario() {
        when(municipioRepository.findAll()).thenReturn(List.of(
                Municipio.builder().coIbge("1").nuLatitude(-10.0).nuLongitude(-50.0).populacao(100_000L).build(),
                Municipio.builder().coIbge("2").nuLatitude(-20.0).nuLongitude(-40.0).populacao(200_000L).build()
        ));
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(ClimaAtualDTO.builder()
                .temperatura(29.0).umidade(85).precipitacao(3.0).build());
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(previsaoConstante(16, 30.0, 8.0, 70));
        when(casoDengueRepository.agregaCasosPorMunicipioNoAno(2024)).thenReturn(List.of(
                new Object[]{"1", 100L},
                new Object[]{"2", 200L}
        ));

        PrevisaoRiscoResponse response = service.calcularRiscoBrasil();

        assertThat(response.getMunicipio()).isEqualTo("Brasil");
        assertThat(response.getClassificacao()).isEqualTo("EPIDEMIA");
        assertThat(response.getIncidencia()).isGreaterThan(0.0);
        assertThat(response.getRisco14Dias()).hasSize(14);
        assertThat(response.getFatores()).isNotEmpty();
    }

    @Test
    void calcularRiscoEstado_retornaNullQuandoNaoHaMunicipios() {
        when(municipioRepository.findBySgUf("XX")).thenReturn(List.of());

        assertThat(service.calcularRiscoEstado("xx")).isNull();
    }

    @Test
    void calcularRiscoEstado_retornaRespostaQuandoHaMunicipios() {
        when(municipioRepository.findBySgUf("MG")).thenReturn(List.of(
                Municipio.builder().coIbge("1").nuLatitude(-18.0).nuLongitude(-44.0).build(),
                Municipio.builder().coIbge("2").nuLatitude(-19.0).nuLongitude(-43.0).build()
        ));
        when(climaService.buscarClimaAtual(anyDouble(), anyDouble())).thenReturn(ClimaAtualDTO.builder()
                .temperatura(26.0).umidade(82).precipitacao(2.0).build());
        when(climaService.buscarPrevisao16Dias(anyDouble(), anyDouble())).thenReturn(previsaoConstante(16, 27.0, 4.0, 65));
        when(casoDengueRepository.agregaCasosPorEstadoNoAno(2024)).thenReturn(List.<Object[]>of(
                new Object[]{"MG", 300L, 100_000L}
        ));

        PrevisaoRiscoResponse response = service.calcularRiscoEstado("mg");

        assertThat(response).isNotNull();
        assertThat(response.getMunicipio()).isEqualTo("Minas Gerais");
        assertThat(response.getIncidencia()).isGreaterThan(0.0);
        assertThat(response.getRisco14Dias()).hasSize(14);
    }

    @Test
    void buscarHospitaisEstado_prefereHospitaisDentroDe100Km() {
        when(municipioRepository.findByCoIbge("3106200")).thenReturn(Optional.of(Municipio.builder()
                .coIbge("3106200").nuLatitude(-19.9).nuLongitude(-43.9).build()));
        when(estabelecimentoRepository.findByEstado("MG")).thenReturn(List.of(
                Estabelecimento.builder().coCnes("1").nuLatitude(-19.91).nuLongitude(-43.91).build(),
                Estabelecimento.builder().coCnes("2").nuLatitude(-21.0).nuLongitude(-45.0).build()
        ));

        List<Estabelecimento> response = service.buscarHospitaisEstado("MG");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getCoCnes()).isEqualTo("1");
    }

    @Test
    void buscarHospitaisEstado_retornaFallbackQuandoNaoHaHospitaisNoRaio() {
        when(municipioRepository.findByCoIbge("3106200")).thenReturn(Optional.of(Municipio.builder()
                .coIbge("3106200").nuLatitude(-19.9).nuLongitude(-43.9).build()));
        when(estabelecimentoRepository.findByEstado("MG")).thenReturn(List.of(
                Estabelecimento.builder().coCnes("1").nuLatitude(-22.0).nuLongitude(-46.0).build(),
                Estabelecimento.builder().coCnes("2").nuLatitude(-23.0).nuLongitude(-47.0).build()
        ));

        List<Estabelecimento> response = service.buscarHospitaisEstado("MG");

        assertThat(response).hasSize(2);
    }

    @Test
    void calcularIncidenciaMediaBrasil_ignoraLinhasInvalidas() {
        when(casoDengueRepository.agregaCasosPorMunicipioNoAno(2024)).thenReturn(List.of(
                new Object[]{"1", 100L},
                new Object[]{"2", 200L},
                new Object[]{null, 50L}
        ));
        when(municipioRepository.findAll()).thenReturn(List.of(
                Municipio.builder().coIbge("1").populacao(100_000L).build(),
                Municipio.builder().coIbge("2").populacao(200_000L).build()
        ));

        double incidencia = service.calcularIncidenciaMediaBrasil(2024);

        assertThat(incidencia).isGreaterThan(0.0);
    }

    @Test
    void calcularIncidenciaMediaEstado_retornaZeroSemDados() {
        when(casoDengueRepository.agregaCasosPorEstadoNoAno(2024)).thenReturn(List.of());

        assertThat(service.calcularIncidenciaMediaEstado("MG", 2024)).isZero();
    }

    private List<PrevisaoDiariaDTO> previsaoConstante(int dias, double temperatura, double chuva, int probabilidade) {
        return IntStream.range(0, dias)
                .mapToObj(i -> PrevisaoDiariaDTO.builder()
                        .data(LocalDate.of(2024, 1, 1).plusDays(i).toString())
                        .temperaturaMaxima(temperatura)
                        .precipitacaoTotal(chuva)
                        .probabilidadeChuva(probabilidade)
                        .build())
                .toList();
    }
}
