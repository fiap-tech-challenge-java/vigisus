package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PressaoOperacionalServiceTest {

    @Mock
    private MunicipioService municipioService;

    @Mock
    private CasoDengueRepository casoDengueRepository;

    @Mock
    private PrevisaoRiscoService previsaoRiscoService;

    @Mock
    private EncaminhamentoService encaminhamentoService;

    @Mock
    private IaService iaService;

    private PressaoOperacionalService service;

    private static final String CO_IBGE = "3131307";
    private static final long POPULACAO = 102_000L;

    private static final Municipio MUNICIPIO = Municipio.builder()
            .coIbge(CO_IBGE)
            .noMunicipio("Lavras")
            .sgUf("MG")
            .nuLatitude(-21.245)
            .nuLongitude(-44.999)
            .populacao(POPULACAO)
            .build();

    @BeforeEach
    void setUp() {
        service = new PressaoOperacionalService(
                municipioService, casoDengueRepository,
                previsaoRiscoService, encaminhamentoService, iaService);

        lenient().when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(MUNICIPIO);
        lenient().when(casoDengueRepository.findCasosPorSemanas(anyString(), anyInt(), any()))
                .thenReturn(List.of());
        lenient().when(casoDengueRepository.sumTotalCasosByCoMunicipioAndAno(anyString(), anyInt()))
                .thenReturn(0L);
        lenient().when(iaService.gerarTextoOperacional(anyString()))
                .thenReturn("Briefing de teste.");
        lenient().when(encaminhamentoService.buscarHospitais(anyString(), anyString(), anyInt()))
                .thenReturn(EncaminhamentoResponse.builder()
                        .coIbge(CO_IBGE)
                        .municipioOrigem("Lavras")
                        .tpLeito("74")
                        .hospitais(List.of())
                        .build());
    }

    // ── testUPAComMuitasSuspeitasEmEpidemiaDeveSerCritica ─────────────────────

    @Test
    void testUPAComMuitasSuspeitasEmEpidemiaDeveSerCritica() {
        // suspeitasDia: 12 → +3
        // classificação: EPIDEMIA → +3
        // tendência: CRESCENTE → +2
        // risco climático: MUITO_ALTO → +2
        // total score = 10 → CRITICO

        // Mock: EPIDEMIA (>300/100k: 307 cases / 102k pop)
        when(casoDengueRepository.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, LocalDate.now().getYear()))
                .thenReturn(307L);

        // Mock: tendência CRESCENTE — semana atual (200) >> semana 3 atrás (100) × 1.2
        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
        when(casoDengueRepository.findCasosPorSemanas(eq(CO_IBGE), anyInt(), any()))
                .thenReturn(buildSemanas(currentWeek, 100L, 110L, 150L, 200L));

        // Mock: risco MUITO_ALTO
        when(previsaoRiscoService.calcularRisco(CO_IBGE)).thenReturn(
                PrevisaoRiscoResponse.builder()
                        .coIbge(CO_IBGE)
                        .municipio("Lavras")
                        .score(7)
                        .classificacao("MUITO_ALTO")
                        .fatores(List.of("Temperatura alta", "Chuva intensa"))
                        .build());

        PressaoOperacionalRequest req = new PressaoOperacionalRequest(CO_IBGE, 12, "UPA");
        PressaoOperacionalResponse resp = service.avaliarPressao(req);

        assertThat(resp.getNivelAtencao()).isEqualTo("CRITICO");
        assertThat(resp.getChecklistInformativo()).isNotEmpty();
        assertThat(resp.getContextoAtual()).isNotBlank();
    }

    // ── testUBSComPoucasSuspeitasContextoBaixoDeveSerNormal ───────────────────

    @Test
    void testUBSComPoucasSuspeitasContextoBaixoDeveSerNormal() {
        // suspeitasDia: 1 → +0
        // classificação: BAIXO (0 casos) → +0
        // tendência: ESTAVEL → +0
        // risco climático: BAIXO (score 0) → +0
        // total score = 0 → NORMAL

        when(casoDengueRepository.sumTotalCasosByCoMunicipioAndAno(anyString(), anyInt()))
                .thenReturn(0L);

        when(previsaoRiscoService.calcularRisco(CO_IBGE)).thenReturn(
                PrevisaoRiscoResponse.builder()
                        .coIbge(CO_IBGE)
                        .municipio("Lavras")
                        .score(0)
                        .classificacao("BAIXO")
                        .fatores(List.of())
                        .build());

        PressaoOperacionalRequest req = new PressaoOperacionalRequest(CO_IBGE, 1, "UBS");
        PressaoOperacionalResponse resp = service.avaliarPressao(req);

        assertThat(resp.getNivelAtencao()).isEqualTo("NORMAL");
        assertThat(resp.getChecklistInformativo()).isNotEmpty();
    }

    // ── testHospitaisReferenciaSaoRetornadosOrdenadosPorDistancia ─────────────

    @Test
    void testHospitaisReferenciaSaoRetornadosOrdenadosPorDistancia() {
        // Hospital próximo (5 km) via clinicos query
        HospitalDTO hospProximo = HospitalDTO.builder()
                .coCnes("CNES001")
                .noFantasia("Hospital Lavras")
                .distanciaKm(5.0)
                .build();
        // Hospital distante (60 km) via UTI query
        HospitalDTO hospDistante = HospitalDTO.builder()
                .coCnes("CNES002")
                .noFantasia("Hospital Varginha")
                .distanciaKm(60.0)
                .build();
        // Hospital médio (30 km) also via clinicos query
        HospitalDTO hospMedio = HospitalDTO.builder()
                .coCnes("CNES003")
                .noFantasia("Hospital Médio")
                .distanciaKm(30.0)
                .build();

        // clinicos returns closest and medium
        when(encaminhamentoService.buscarHospitais(CO_IBGE, "74", 10))
                .thenReturn(EncaminhamentoResponse.builder()
                        .coIbge(CO_IBGE)
                        .hospitais(List.of(hospProximo, hospMedio))
                        .build());
        // UTI returns distant
        when(encaminhamentoService.buscarHospitais(CO_IBGE, "81", 5))
                .thenReturn(EncaminhamentoResponse.builder()
                        .coIbge(CO_IBGE)
                        .hospitais(List.of(hospDistante))
                        .build());

        List<HospitalDTO> result = service.buscarHospitaisReferencia(CO_IBGE);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getDistanciaKm()).isLessThan(result.get(1).getDistanciaKm());
        assertThat(result.get(1).getDistanciaKm()).isLessThan(result.get(2).getDistanciaKm());
        assertThat(result.get(0).getNoFantasia()).isEqualTo("Hospital Lavras");
    }

    // ── testNivelAtencaoCalculation (unit tests for scoring) ──────────────────

    @Test
    void testScoreApenasComSuspeitasAltas_deveSerNormal() {
        // suspeitasDia: 7 → +2; classificação BAIXO → +0; tendência ESTAVEL → +0; sem risco → +0
        // score = 2 → NORMAL
        PrevisaoRiscoResponse riscoBaixo = PrevisaoRiscoResponse.builder()
                .score(0).classificacao("BAIXO").fatores(List.of()).build();
        String nivel = service.calcularNivelAtencao(7, "BAIXO", "ESTAVEL", riscoBaixo);
        assertThat(nivel).isEqualTo("NORMAL");
    }

    @Test
    void testScoreComSuspeitasEEpidemia_deveSerElevado() {
        // suspeitasDia: 5 → +2; EPIDEMIA → +3; score = 5 → ELEVADO
        PrevisaoRiscoResponse risco = PrevisaoRiscoResponse.builder()
                .score(0).classificacao("BAIXO").fatores(List.of()).build();
        String nivel = service.calcularNivelAtencao(5, "EPIDEMIA", "ESTAVEL", risco);
        assertThat(nivel).isEqualTo("ELEVADO");
    }

    @Test
    void testScoreCriticoComTodosOsFatores() {
        // suspeitasDia: 10 → +3; EPIDEMIA → +3; CRESCENTE → +2; MUITO_ALTO → +2; score=10 → CRITICO
        PrevisaoRiscoResponse risco = PrevisaoRiscoResponse.builder()
                .score(7).classificacao("MUITO_ALTO").fatores(List.of()).build();
        String nivel = service.calcularNivelAtencao(10, "EPIDEMIA", "CRESCENTE", risco);
        assertThat(nivel).isEqualTo("CRITICO");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds mock result rows for findCasosPorSemanas returning 4 weeks.
     * weeks[0] = 3 weeks ago ... weeks[3] = current week
     */
    private List<Object[]> buildSemanas(int currentWeek, long w3, long w2, long w1, long w0) {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{currentWeek - 3, w3});
        rows.add(new Object[]{currentWeek - 2, w2});
        rows.add(new Object[]{currentWeek - 1, w1});
        rows.add(new Object[]{currentWeek, w0});
        return rows;
    }
}
