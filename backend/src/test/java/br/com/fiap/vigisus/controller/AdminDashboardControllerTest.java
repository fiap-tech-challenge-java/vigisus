package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.service.AdminMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDashboardControllerTest {

    private AdminMetricsService metricsService;
    private AdminDashboardController controller;

    @BeforeEach
    void setUp() {
        metricsService = new AdminMetricsService(new SimpleMeterRegistry());
        controller = new AdminDashboardController(metricsService);
    }

    @Test
    void resumo_retornaKpisComTimestamp() {
        metricsService.registrarBusca("Campinas", "SP");
        metricsService.registrarBuscaIa("dengue em SP");
        metricsService.registrarTriagem();
        metricsService.registrarCacheHit();

        var response = controller.resumo();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKey("buscas_total");
        assertThat(body).containsKey("buscas_ia");
        assertThat(body).containsKey("triagens");
        assertThat(body).containsKey("cache_hits");
        assertThat(body).containsKey("timestamp");
        assertThat((Long) body.get("buscas_total")).isEqualTo(1L);
        assertThat((Long) body.get("buscas_ia")).isEqualTo(1L);
        assertThat((Long) body.get("triagens")).isEqualTo(1L);
        assertThat((Long) body.get("cache_hits")).isEqualTo(1L);
    }

    @Test
    void topMunicipios_retornaListaComMunicipiosConsultados() {
        metricsService.registrarBusca("Campinas", "SP");
        metricsService.registrarBusca("Campinas", "SP");
        metricsService.registrarBusca("São Paulo", "SP");

        var response = controller.topMunicipios(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> body = response.getBody();
        assertThat(body).isNotNull().isNotEmpty();
        assertThat(body.get(0).get("nome")).isEqualTo("Campinas/SP");
        assertThat(body.get(0).get("total")).isEqualTo(2L);
    }

    @Test
    void topMunicipios_retornaListaVaziaQuandoSemConsultas() {
        var response = controller.topMunicipios(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    @Test
    void topEstados_retornaRankingDeEstados() {
        metricsService.registrarBusca("Campinas", "SP");
        metricsService.registrarBusca("Santos", "SP");
        metricsService.registrarBusca("Rio de Janeiro", "RJ");

        var response = controller.topEstados(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> body = response.getBody();
        assertThat(body).isNotNull().isNotEmpty();
        assertThat(body.get(0).get("nome")).isEqualTo("SP");
        assertThat(body.get(0).get("total")).isEqualTo(2L);
    }

    @Test
    void buscasIa_retornaPerguntas() {
        metricsService.registrarBuscaIa("dengue em campinas");
        metricsService.registrarBuscaIa("dengue em campinas");
        metricsService.registrarBuscaIa("febre amarela em sp");

        var response = controller.buscasIa(20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> body = response.getBody();
        assertThat(body).isNotNull().isNotEmpty();
        assertThat(body.get(0).get("nome")).isEqualTo("dengue em campinas");
        assertThat(body.get(0).get("total")).isEqualTo(2L);
    }

    @Test
    void municipiosRisco_retornaListaDeMunicipios() {
        metricsService.registrarBusca("Manaus", "AM");

        var response = controller.municipiosRisco(50);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isNotEmpty();
    }

    @Test
    void dashboard_retornaHtml() {
        var response = controller.dashboard();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
        assertThat(response.getBody()).contains("<title>VigiSUS");
        assertThat(response.getBody()).contains("/admin/resumo");
    }
}
