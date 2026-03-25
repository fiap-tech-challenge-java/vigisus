package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.operacional.ConstruirContextoEpidemiologicoOperacional;
import br.com.fiap.vigisus.application.operacional.MescladorHospitaisReferencia;
import br.com.fiap.vigisus.application.operacional.MontadorContextoOperacional;
import br.com.fiap.vigisus.application.operacional.MontadorPrevisaoOperacional;
import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.application.port.MunicipioPort;
import br.com.fiap.vigisus.domain.epidemiologia.ClassificacaoEpidemiologicaPolicy;
import br.com.fiap.vigisus.domain.epidemiologia.ComparativoHistoricoEpidemiologicoPolicy;
import br.com.fiap.vigisus.domain.operacional.CalculadoraNivelAtencaoOperacional;
import br.com.fiap.vigisus.domain.operacional.CalculadoraTendenciaOperacional;
import br.com.fiap.vigisus.domain.operacional.ChecklistOperacionalPolicy;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import br.com.fiap.vigisus.model.Municipio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private CasoDenguePort casoDenguePort;

    @Mock
    private MunicipioPort municipioPort;

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
                municipioService,
                previsaoRiscoService,
                encaminhamentoService,
                iaService,
                new CalculadoraNivelAtencaoOperacional(),
                new MontadorPrevisaoOperacional(),
                new MontadorContextoOperacional(),
                new ChecklistOperacionalPolicy(),
                new MescladorHospitaisReferencia(),
                new ConstruirContextoEpidemiologicoOperacional(
                        casoDenguePort,
                        municipioPort,
                        new ClassificacaoEpidemiologicaPolicy(),
                        new ComparativoHistoricoEpidemiologicoPolicy(),
                        new CalculadoraTendenciaOperacional()
                )
        );

        lenient().when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(MUNICIPIO);
        lenient().when(municipioPort.findByCoIbge(CO_IBGE)).thenReturn(Optional.of(MUNICIPIO));
        lenient().when(casoDenguePort.findCasosPorSemanas(anyString(), anyInt(), any())).thenReturn(List.of());
        lenient().when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(anyString(), anyInt())).thenReturn(0L);
        lenient().when(iaService.gerarTextoOperacional(anyString())).thenReturn("Briefing de teste.");
        lenient().when(encaminhamentoService.buscarHospitais(anyString(), anyString(), anyInt()))
                .thenReturn(EncaminhamentoResponse.builder().coIbge(CO_IBGE).municipioOrigem("Lavras").tpLeito("74").hospitais(List.of()).build());
    }

    @Test
    void testUPAComMuitasSuspeitasEmEpidemiaDeveSerCritica() {
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, LocalDate.now().getYear())).thenReturn(307L);
        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), anyInt(), any()))
                .thenReturn(buildSemanas(currentWeek, 100L, 110L, 150L, 200L));
        when(previsaoRiscoService.calcularRisco(CO_IBGE)).thenReturn(
                PrevisaoRiscoResponse.builder()
                        .coIbge(CO_IBGE)
                        .municipio("Lavras")
                        .score(7)
                        .classificacao("MUITO_ALTO")
                        .fatores(List.of("Temperatura alta", "Chuva intensa"))
                        .build());

        PressaoOperacionalResponse resp = service.avaliarPressao(new PressaoOperacionalRequest(CO_IBGE, 12, "UPA"));

        assertThat(resp.getNivelAtencao()).isEqualTo("CRITICO");
        assertThat(resp.getChecklistInformativo()).isNotEmpty();
        assertThat(resp.getContextoAtual()).isNotBlank();
    }

    @Test
    void testUBSComPoucasSuspeitasContextoBaixoDeveSerNormal() {
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(anyString(), anyInt())).thenReturn(0L);
        when(previsaoRiscoService.calcularRisco(CO_IBGE)).thenReturn(
                PrevisaoRiscoResponse.builder().coIbge(CO_IBGE).municipio("Lavras").score(0).classificacao("BAIXO").fatores(List.of()).build());

        PressaoOperacionalResponse resp = service.avaliarPressao(new PressaoOperacionalRequest(CO_IBGE, 1, "UBS"));

        assertThat(resp.getNivelAtencao()).isEqualTo("NORMAL");
        assertThat(resp.getChecklistInformativo()).isNotEmpty();
    }

    @Test
    void testHospitaisReferenciaSaoRetornadosOrdenadosPorDistancia() {
        HospitalDTO hospProximo = HospitalDTO.builder().coCnes("CNES001").noFantasia("Hospital Lavras").distanciaKm(5.0).build();
        HospitalDTO hospDistante = HospitalDTO.builder().coCnes("CNES002").noFantasia("Hospital Varginha").distanciaKm(60.0).build();
        HospitalDTO hospMedio = HospitalDTO.builder().coCnes("CNES003").noFantasia("Hospital Medio").distanciaKm(30.0).build();

        when(encaminhamentoService.buscarHospitais(CO_IBGE, "74", 10))
                .thenReturn(EncaminhamentoResponse.builder().coIbge(CO_IBGE).hospitais(List.of(hospProximo, hospMedio)).build());
        when(encaminhamentoService.buscarHospitais(CO_IBGE, "81", 5))
                .thenReturn(EncaminhamentoResponse.builder().coIbge(CO_IBGE).hospitais(List.of(hospDistante)).build());

        List<HospitalDTO> result = service.buscarHospitaisReferencia(CO_IBGE);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getDistanciaKm()).isLessThan(result.get(1).getDistanciaKm());
        assertThat(result.get(1).getDistanciaKm()).isLessThan(result.get(2).getDistanciaKm());
        assertThat(result.get(0).getNoFantasia()).isEqualTo("Hospital Lavras");
    }

    @Test
    void buscarHospitaisReferencia_removeDuplicadosELimitaATres() {
        HospitalDTO h1 = HospitalDTO.builder().coCnes("1").noFantasia("H1").distanciaKm(15.0).build();
        HospitalDTO h2 = HospitalDTO.builder().coCnes("2").noFantasia("H2").distanciaKm(5.0).build();
        HospitalDTO h3 = HospitalDTO.builder().coCnes("3").noFantasia("H3").distanciaKm(25.0).build();
        HospitalDTO h4 = HospitalDTO.builder().coCnes("4").noFantasia("H4").distanciaKm(35.0).build();
        when(encaminhamentoService.buscarHospitais(CO_IBGE, "74", 10))
                .thenReturn(EncaminhamentoResponse.builder().coIbge(CO_IBGE).hospitais(List.of(h1, h2)).build());
        when(encaminhamentoService.buscarHospitais(CO_IBGE, "81", 5))
                .thenReturn(EncaminhamentoResponse.builder().coIbge(CO_IBGE).hospitais(List.of(h2, h3, h4)).build());

        List<HospitalDTO> result = service.buscarHospitaisReferencia(CO_IBGE);

        assertThat(result).extracting(HospitalDTO::getCoCnes).containsExactly("2", "1", "3");
    }

    @Test
    void construirContexto_quandoAnoAnteriorSemDados_retornaComparativoInsuficienteECrescente() {
        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear()), any()))
                .thenReturn(buildSemanas(currentWeek, 0L, 0L, 0L, 5L));
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear() - 1), any()))
                .thenReturn(List.of());
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, LocalDate.now().getYear())).thenReturn(0L);

        PressaoOperacionalResponse.ContextoEpidemiologicoDTO contexto = service.construirContexto(CO_IBGE);

        assertThat(contexto.getClassificacaoAtual()).isEqualTo("BAIXO");
        assertThat(contexto.getTendencia()).isEqualTo("CRESCENTE");
        assertThat(contexto.getCasosUltimasSemanas()).isEqualTo(5);
        assertThat(contexto.getComparativoHistorico()).contains("registros");
    }

    @Test
    void construirContexto_quandoMesmoVolume_retornaComparativoSemelhanteEEstavel() {
        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear()), any()))
                .thenReturn(buildSemanas(currentWeek, 10L, 10L, 10L, 10L));
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear() - 1), any()))
                .thenReturn(buildSemanas(currentWeek, 10L, 10L, 10L, 10L));
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, LocalDate.now().getYear())).thenReturn(0L);

        PressaoOperacionalResponse.ContextoEpidemiologicoDTO contexto = service.construirContexto(CO_IBGE);

        assertThat(contexto.getTendencia()).isEqualTo("ESTAVEL");
        assertThat(contexto.getComparativoHistorico()).contains("semelhante");
    }

    @Test
    void avaliarPressao_quandoRiscoIndisponivel_usaFallbackNaPrevisaoEPadraoHistorico() {
        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear()), any()))
                .thenReturn(buildSemanas(currentWeek, 1L, 1L, 1L, 1L));
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear() - 1), any()))
                .thenReturn(List.of());
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, LocalDate.now().getYear())).thenReturn(40L);
        when(previsaoRiscoService.calcularRisco(CO_IBGE)).thenThrow(new RuntimeException("sem clima"));

        PressaoOperacionalResponse resp = service.avaliarPressao(new PressaoOperacionalRequest(CO_IBGE, 3, "UBS"));

        assertThat(resp.getPrevisao().getRiscoClimatico()).contains("Indispon");
        assertThat(resp.getPrevisao().getTendencia7Dias()).contains("dispon");
        assertThat(resp.getPadraoHistorico()).contains("insuficientes");
        assertThat(resp.getNivelAtencao()).isEqualTo("NORMAL");
    }

    @Test
    void avaliarPressao_quandoMunicipioFalhaNaClassificacao_retornaBaixoNoContexto() {
        String outroIbge = "9999999";
        Municipio outro = Municipio.builder().coIbge(outroIbge).noMunicipio("Cidade X").sgUf("MG").build();
        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
        when(municipioService.buscarPorCoIbge(outroIbge)).thenReturn(outro);
        when(municipioPort.findByCoIbge(outroIbge)).thenThrow(new RuntimeException("sem populacao"));
        when(casoDenguePort.findCasosPorSemanas(eq(outroIbge), eq(LocalDate.now().getYear()), any()))
                .thenReturn(buildSemanas(currentWeek, 2L, 2L, 2L, 2L));
        when(casoDenguePort.findCasosPorSemanas(eq(outroIbge), eq(LocalDate.now().getYear() - 1), any()))
                .thenReturn(List.of());
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(outroIbge, LocalDate.now().getYear())).thenReturn(80L);
        when(previsaoRiscoService.calcularRisco(outroIbge)).thenReturn(
                PrevisaoRiscoResponse.builder().coIbge(outroIbge).municipio("Cidade X").score(0).classificacao("BAIXO").fatores(List.of()).build());

        PressaoOperacionalResponse resp = service.avaliarPressao(new PressaoOperacionalRequest(outroIbge, 0, "UBS"));

        assertThat(resp.getContexto().getClassificacaoAtual()).isEqualTo("BAIXO");
    }

    @Test
    void testScoreApenasComSuspeitasAltas_deveSerNormal() {
        PrevisaoRiscoResponse risco = PrevisaoRiscoResponse.builder().score(0).classificacao("BAIXO").fatores(List.of()).build();
        String nivel = service.calcularNivelAtencao(7, "BAIXO", "ESTAVEL", risco);
        assertThat(nivel).isEqualTo("NORMAL");
    }

    @Test
    void testScoreComSuspeitasEEpidemia_deveSerElevado() {
        PrevisaoRiscoResponse risco = PrevisaoRiscoResponse.builder().score(0).classificacao("BAIXO").fatores(List.of()).build();
        String nivel = service.calcularNivelAtencao(5, "EPIDEMIA", "ESTAVEL", risco);
        assertThat(nivel).isEqualTo("ELEVADO");
    }

    @Test
    void testScoreCriticoComTodosOsFatores() {
        PrevisaoRiscoResponse risco = PrevisaoRiscoResponse.builder().score(7).classificacao("MUITO_ALTO").fatores(List.of()).build();
        String nivel = service.calcularNivelAtencao(10, "EPIDEMIA", "CRESCENTE", risco);
        assertThat(nivel).isEqualTo("CRITICO");
    }

    @Test
    void calcularNivelAtencao_comRiscoAltoEAlertaModerado_retornaElevado() {
        PrevisaoRiscoResponse risco = PrevisaoRiscoResponse.builder().score(5).classificacao("ALTO").fatores(List.of()).build();
        String nivel = service.calcularNivelAtencao(2, "MODERADO", "ESTAVEL", risco);
        assertThat(nivel).isEqualTo("ELEVADO");
    }

    private List<Object[]> buildSemanas(int currentWeek, long w3, long w2, long w1, long w0) {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{currentWeek - 3, w3});
        rows.add(new Object[]{currentWeek - 2, w2});
        rows.add(new Object[]{currentWeek - 1, w1});
        rows.add(new Object[]{currentWeek, w0});
        return rows;
    }
}
