package br.com.fiap.vigisus.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDashboardControllerTest {

    private SimpleMeterRegistry meterRegistry;
    private AdminDashboardController controller;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        controller = new AdminDashboardController(meterRegistry);
    }

    @Test
    void getTopMunicipios_retornaListaVaziaQuandoSemBuscas() {
        List<Map<String, Object>> result = controller.getTopMunicipios();
        assertThat(result).isEmpty();
    }

    @Test
    void getTopMunicipios_retornaMunicipiosOrdenadosPorBuscas() {
        Counter.builder("busca.municipio").tag("municipio", "Campinas").tag("uf", "SP").register(meterRegistry).increment();
        Counter.builder("busca.municipio").tag("municipio", "Campinas").tag("uf", "SP").register(meterRegistry).increment();
        Counter.builder("busca.municipio").tag("municipio", "Sao Paulo").tag("uf", "SP").register(meterRegistry).increment();

        List<Map<String, Object>> result = controller.getTopMunicipios();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("municipio")).isEqualTo("Campinas");
        assertThat(result.get(0).get("buscas")).isEqualTo(2L);
        assertThat(result.get(1).get("municipio")).isEqualTo("Sao Paulo");
        assertThat(result.get(1).get("buscas")).isEqualTo(1L);
    }

    @Test
    void getTopMunicipios_limita10Resultados() {
        for (int i = 1; i <= 15; i++) {
            Counter.builder("busca.municipio")
                    .tag("municipio", "Municipio" + i)
                    .tag("uf", "SP")
                    .register(meterRegistry)
                    .increment();
        }

        List<Map<String, Object>> result = controller.getTopMunicipios();

        assertThat(result).hasSize(10);
    }

    @Test
    void getTopMunicipios_incluiCampoUf() {
        Counter.builder("busca.municipio").tag("municipio", "Manaus").tag("uf", "AM").register(meterRegistry).increment();

        List<Map<String, Object>> result = controller.getTopMunicipios();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("uf")).isEqualTo("AM");
    }
}
