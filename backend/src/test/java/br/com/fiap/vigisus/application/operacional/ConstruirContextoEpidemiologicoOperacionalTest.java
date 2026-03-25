package br.com.fiap.vigisus.application.operacional;

import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.application.port.MunicipioPort;
import br.com.fiap.vigisus.domain.epidemiologia.ClassificacaoEpidemiologicaPolicy;
import br.com.fiap.vigisus.domain.epidemiologia.ComparativoHistoricoEpidemiologicoPolicy;
import br.com.fiap.vigisus.domain.operacional.CalculadoraTendenciaOperacional;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.ContextoEpidemiologicoDTO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConstruirContextoEpidemiologicoOperacionalTest {

    @Mock
    private CasoDenguePort casoDenguePort;

    @Mock
    private MunicipioPort municipioPort;

    private ConstruirContextoEpidemiologicoOperacional construtor;

    private static final String CO_IBGE = "3131307";

    @BeforeEach
    void setUp() {
        construtor = new ConstruirContextoEpidemiologicoOperacional(
                casoDenguePort,
                municipioPort,
                new ClassificacaoEpidemiologicaPolicy(),
                new ComparativoHistoricoEpidemiologicoPolicy(),
                new CalculadoraTendenciaOperacional()
        );
    }

    @Test
    void executar_quandoSemAnoAnterior_retornaComparativoInsuficienteECrescente() {
        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear()), any()))
                .thenReturn(buildSemanas(currentWeek, 0L, 0L, 0L, 5L));
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear() - 1), any()))
                .thenReturn(List.of());
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, LocalDate.now().getYear())).thenReturn(0L);

        ContextoEpidemiologicoDTO contexto = construtor.executar(CO_IBGE);

        assertThat(contexto.getClassificacaoAtual()).isEqualTo("BAIXO");
        assertThat(contexto.getTendencia()).isEqualTo("CRESCENTE");
        assertThat(contexto.getCasosUltimasSemanas()).isEqualTo(5);
        assertThat(contexto.getComparativoHistorico()).contains("registros");
    }

    @Test
    void executar_quandoMesmoVolume_retornaComparativoSemelhanteEEstavel() {
        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear()), any()))
                .thenReturn(buildSemanas(currentWeek, 10L, 10L, 10L, 10L));
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear() - 1), any()))
                .thenReturn(buildSemanas(currentWeek, 10L, 10L, 10L, 10L));
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, LocalDate.now().getYear())).thenReturn(0L);

        ContextoEpidemiologicoDTO contexto = construtor.executar(CO_IBGE);

        assertThat(contexto.getTendencia()).isEqualTo("ESTAVEL");
        assertThat(contexto.getComparativoHistorico()).contains("semelhante");
    }

    @Test
    void executar_quandoFalhaNoMunicipio_retornaClassificacaoBaixa() {
        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear()), any()))
                .thenReturn(buildSemanas(currentWeek, 2L, 2L, 2L, 2L));
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear() - 1), any()))
                .thenReturn(List.of());
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, LocalDate.now().getYear())).thenReturn(80L);
        when(municipioPort.findByCoIbge(CO_IBGE)).thenThrow(new RuntimeException("sem populacao"));

        ContextoEpidemiologicoDTO contexto = construtor.executar(CO_IBGE);

        assertThat(contexto.getClassificacaoAtual()).isEqualTo("BAIXO");
    }

    @Test
    void executar_quandoTemMunicipioClassificaPorIncidencia() {
        Municipio municipio = Municipio.builder().coIbge(CO_IBGE).populacao(100_000L).build();
        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear()), any()))
                .thenReturn(buildSemanas(currentWeek, 1L, 1L, 1L, 1L));
        when(casoDenguePort.findCasosPorSemanas(eq(CO_IBGE), eq(LocalDate.now().getYear() - 1), any()))
                .thenReturn(List.of());
        when(casoDenguePort.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, LocalDate.now().getYear())).thenReturn(120L);
        when(municipioPort.findByCoIbge(CO_IBGE)).thenReturn(Optional.of(municipio));

        ContextoEpidemiologicoDTO contexto = construtor.executar(CO_IBGE);

        assertThat(contexto.getClassificacaoAtual()).isEqualTo("ALTO");
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
