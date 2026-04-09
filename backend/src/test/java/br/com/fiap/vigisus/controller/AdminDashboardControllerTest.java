package br.com.fiap.vigisus.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@WithMockUser
class AdminDashboardControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean MeterRegistry meterRegistry;

    @Test
    void deveRetornarResumoVazioSemMedidas() throws Exception {
        when(meterRegistry.find(any())).thenReturn(Search.in(meterRegistry));

        mockMvc.perform(get("/admin/resumo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.buscas_total").isNumber())
            .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void deveRetornarListaVaziaParaTopMunicipios() throws Exception {
        when(meterRegistry.find("vigisus.buscas.municipio"))
            .thenReturn(Search.in(meterRegistry));

        mockMvc.perform(get("/admin/top-municipios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
