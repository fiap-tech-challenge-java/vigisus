package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.domain.epidemiologia.CalculadoraTendenciaEpidemiologica;
import br.com.fiap.vigisus.domain.epidemiologia.ClassificacaoEpidemiologicaPolicy;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.exception.DadosInsuficientesException;
import br.com.fiap.vigisus.exception.RecursoNaoEncontradoException;
import br.com.fiap.vigisus.model.CasoDengue;
import br.com.fiap.vigisus.model.Municipio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilEpidemiologicoServiceTest {

    @Mock
    private MunicipioService municipioService;

    @Mock
    private CasoDenguePort casoDenguePort;

    @Mock
    private RankingService rankingService;

    private PerfilEpidemiologicoService service;

    private static final String CO_IBGE = "3131307";
    private static final long POPULACAO = 102_000L;

    @BeforeEach
    void setUp() {
        service = new PerfilEpidemiologicoService(
                municipioService,
                casoDenguePort,
                rankingService,
                new ClassificacaoEpidemiologicaPolicy(),
                new CalculadoraTendenciaEpidemiologica()
        );
        lenient().when(rankingService.calcularPosicaoNoEstado(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("1 de 10");
        lenient().when(casoDenguePort.findByCoMunicipioAndAnoOrderBySemanaEpiAsc(anyString(), anyInt()))
                .thenReturn(List.of());
        lenient().when(municipioService.listarPorUf(anyString()))
                .thenReturn(List.of());
    }

    @Test
    void testCalculaIncidenciaCorretamente() {
        Municipio municipio = municipioBase();
        when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(municipio);
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, 2024)).thenReturn(306L);

        PerfilEpidemiologicoResponse response = service.gerarPerfil(CO_IBGE, "dengue", 2024);

        double expectedIncidencia = 306.0 / POPULACAO * 100_000;
        assertThat(response.getIncidencia()).isCloseTo(expectedIncidencia, within(0.01));
        assertThat(response.getTotal()).isEqualTo(306L);
    }

    @Test
    void testClassificaEpidemiaAcimaDe300por100k() {
        when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(municipioBase());
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, 2024)).thenReturn(307L);

        PerfilEpidemiologicoResponse response = service.gerarPerfil(CO_IBGE, "dengue", 2024);

        assertThat(response.getIncidencia()).isGreaterThan(300.0);
        assertThat(response.getClassificacao()).isEqualTo("EPIDEMIA");
    }

    @Test
    void testLancaDadosInsuficientesQuandoSemCasos() {
        when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(municipioBase());
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, 2024)).thenReturn(0L);

        assertThatThrownBy(() -> service.gerarPerfil(CO_IBGE, "dengue", 2024))
                .isInstanceOf(DadosInsuficientesException.class)
                .hasMessage("Dados insuficientes para Lavras no ano 2024");
    }

    @Test
    void gerarPerfil_lancaQuandoPopulacaoNaoDisponivel() {
        Municipio municipio = Municipio.builder()
                .coIbge(CO_IBGE)
                .noMunicipio("Lavras")
                .sgUf("MG")
                .populacao(0L)
                .nuLatitude(-21.245)
                .nuLongitude(-44.999)
                .build();
        when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(municipio);
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, 2024)).thenReturn(10L);

        assertThatThrownBy(() -> service.gerarPerfil(CO_IBGE, "dengue", 2024))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Popula");
    }

    @Test
    void gerarPerfil_montaCamposDerivadosSemanasTendenciaEComparativos() {
        Municipio lavras = municipioBase();
        Municipio alfenas = Municipio.builder().coIbge("3101607").noMunicipio("Alfenas").sgUf("MG").populacao(80_000L).build();
        Municipio varginha = Municipio.builder().coIbge("3170701").noMunicipio("Varginha").sgUf("MG").populacao(120_000L).build();

        when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(lavras);
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, 2024)).thenReturn(204L);
        when(rankingService.calcularPosicaoNoEstado(CO_IBGE, "MG", "dengue", 2024)).thenReturn("3 de 50");
        when(casoDenguePort.findByCoMunicipioAndAnoOrderBySemanaEpiAsc(CO_IBGE, 2024)).thenReturn(List.of(
                caso(2024, 1, 5L),
                caso(2024, 2, 5L),
                caso(2024, 3, 5L),
                caso(2024, 4, 5L),
                caso(2024, 5, 10L),
                caso(2024, 6, 10L),
                caso(2024, 7, 10L),
                caso(2024, 8, 20L)
        ));
        when(casoDenguePort.findByCoMunicipioAndAnoOrderBySemanaEpiAsc(CO_IBGE, 2023)).thenReturn(List.of(
                caso(2023, 1, null),
                caso(2023, 2, 7L)
        ));
        when(municipioService.listarPorUf("MG")).thenReturn(List.of(lavras, alfenas, varginha));
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno("3101607", 2024)).thenReturn(80L);
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno("3170701", 2024)).thenReturn(0L);

        PerfilEpidemiologicoResponse response = service.gerarPerfil(CO_IBGE, "dengue", 2024);

        assertThat(response.getComparativoEstado()).isNotNull();
        assertThat(response.getComparativoEstado().getPosicaoRankingEstado()).isEqualTo("3 de 50");
        assertThat(response.getTendencia()).isEqualTo("CRESCENTE");
        assertThat(response.getSemanas()).hasSize(8);
        assertThat(response.getSemanasAnoAnterior()).hasSize(2);
        assertThat(response.getSemanasAnoAnterior().get(0).getCasos()).isZero();
        assertThat(response.getIncidenciaMediaEstado()).isCloseTo(150.0, within(0.01));
        assertThat(response.getPosicaoEstado()).isEqualTo("3 de 50 municípios em MG");
        assertThat(response.getUf()).isEqualTo("MG");
        assertThat(response.getNuLatitude()).isEqualTo(-21.245);
        assertThat(response.getNuLongitude()).isEqualTo(-44.999);
    }

    @Test
    void gerarPerfil_quandoSemRankingEMediaEstadual_retornaNulosEEstavel() {
        when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(municipioBase());
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, 2024)).thenReturn(100L);
        when(rankingService.calcularPosicaoNoEstado(CO_IBGE, "MG", "dengue", 2024)).thenReturn(null);
        when(casoDenguePort.findByCoMunicipioAndAnoOrderBySemanaEpiAsc(CO_IBGE, 2024)).thenReturn(List.of(
                caso(2024, 1, 2L),
                caso(2024, 2, 0L),
                caso(2024, 3, 1L),
                caso(2024, 4, 0L)
        ));
        when(casoDenguePort.findByCoMunicipioAndAnoOrderBySemanaEpiAsc(CO_IBGE, 2023)).thenReturn(List.of());
        when(municipioService.listarPorUf("MG")).thenReturn(List.of(
                municipioBase(),
                Municipio.builder().coIbge("1").noMunicipio("Sem Pop").sgUf("MG").populacao(0L).build()
        ));

        PerfilEpidemiologicoResponse response = service.gerarPerfil(CO_IBGE, "dengue", 2024);

        assertThat(response.getComparativoEstado()).isNull();
        assertThat(response.getPosicaoEstado()).isNull();
        assertThat(response.getTendencia()).isEqualTo("ESTAVEL");
        assertThat(response.getIncidenciaMediaEstado()).isCloseTo(98.039, within(0.01));
    }

    @Test
    void gerarPerfil_detectaTendenciaDecrescente() {
        when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(municipioBase());
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, 2024)).thenReturn(120L);
        when(casoDenguePort.findByCoMunicipioAndAnoOrderBySemanaEpiAsc(CO_IBGE, 2024)).thenReturn(List.of(
                caso(2024, 1, 20L),
                caso(2024, 2, 20L),
                caso(2024, 3, 20L),
                caso(2024, 4, 20L),
                caso(2024, 5, 5L),
                caso(2024, 6, 5L),
                caso(2024, 7, 5L),
                caso(2024, 8, 5L)
        ));

        PerfilEpidemiologicoResponse response = service.gerarPerfil(CO_IBGE, "dengue", 2024);

        assertThat(response.getTendencia()).isEqualTo("DECRESCENTE");
    }

    @ParameterizedTest
    @CsvSource({
            "0.0,BAIXO",
            "49.99,BAIXO",
            "50.0,MODERADO",
            "99.99,MODERADO",
            "100.0,ALTO",
            "300.0,ALTO",
            "300.01,EPIDEMIA",
            "500.0,EPIDEMIA"
    })
    void classificar_thresholds(double incidencia, String expected) {
        String result = new ClassificacaoEpidemiologicaPolicy().classificar(incidencia);
        assertThat(result).isEqualTo(expected);
    }

    private Municipio municipioBase() {
        return Municipio.builder()
                .coIbge(CO_IBGE)
                .noMunicipio("Lavras")
                .sgUf("MG")
                .populacao(POPULACAO)
                .nuLatitude(-21.245)
                .nuLongitude(-44.999)
                .build();
    }

    private CasoDengue caso(int ano, int semana, Long totalCasos) {
        return CasoDengue.builder()
                .coMunicipio(CO_IBGE)
                .ano(ano)
                .semanaEpi(semana)
                .totalCasos(totalCasos)
                .build();
    }
}
