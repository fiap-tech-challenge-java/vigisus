package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.AdminBuscaIaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IaBuscaTrackerTest {

    private IaBuscaTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new IaBuscaTracker();
    }

    @Test
    void registrar_contabilizaConsultasUnicas() {
        tracker.registrar("dengue em SP");
        tracker.registrar("dengue em SP");
        tracker.registrar("risco no RJ");

        List<AdminBuscaIaDTO> resultado = tracker.listarMaisFrequentes(10);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getPergunta()).isEqualTo("dengue em sp");
        assertThat(resultado.get(0).getContagem()).isEqualTo(2);
    }

    @Test
    void listarMaisFrequentes_retornaOrdenadoDecrescente() {
        tracker.registrar("a");
        tracker.registrar("b");
        tracker.registrar("b");
        tracker.registrar("b");
        tracker.registrar("a");
        tracker.registrar("c");

        List<AdminBuscaIaDTO> resultado = tracker.listarMaisFrequentes(10);

        assertThat(resultado.get(0).getContagem()).isGreaterThanOrEqualTo(resultado.get(1).getContagem());
    }

    @Test
    void listarMaisFrequentes_respeitaLimite() {
        for (int i = 0; i < 30; i++) {
            tracker.registrar("pergunta " + i);
        }

        List<AdminBuscaIaDTO> resultado = tracker.listarMaisFrequentes(5);

        assertThat(resultado).hasSize(5);
    }

    @Test
    void registrar_ignoraPerguntasNulasEVazias() {
        tracker.registrar(null);
        tracker.registrar("");
        tracker.registrar("   ");

        assertThat(tracker.listarMaisFrequentes(10)).isEmpty();
    }

    @Test
    void registrar_normalizaParaMinusculasERemoveEspacos() {
        tracker.registrar("  Dengue em SP  ");
        tracker.registrar("dengue em sp");

        List<AdminBuscaIaDTO> resultado = tracker.listarMaisFrequentes(10);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getContagem()).isEqualTo(2);
    }

    @Test
    void registrar_truncaPerguntasLongas() {
        String longa = "a".repeat(200);
        tracker.registrar(longa);
        tracker.registrar(longa);

        List<AdminBuscaIaDTO> resultado = tracker.listarMaisFrequentes(10);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPergunta().length()).isLessThanOrEqualTo(120);
        assertThat(resultado.get(0).getContagem()).isEqualTo(2);
    }

    @Test
    void listarMaisFrequentes_retornaUltimaConsulta() {
        tracker.registrar("consulta teste");

        List<AdminBuscaIaDTO> resultado = tracker.listarMaisFrequentes(10);

        assertThat(resultado.get(0).getUltimaConsulta()).isNotNull().isNotBlank();
    }
}
