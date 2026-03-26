package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarPerfilEpidemiologicoUseCase;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.exception.MunicipioNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PerfilControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultarPerfilEpidemiologicoUseCase consultarPerfilEpidemiologicoUseCase;

    @Test
    void deveRetornar404ParaMunicipioInexistente() throws Exception {
        when(consultarPerfilEpidemiologicoUseCase.buscarMunicipio(eq("9999999"), any(), any()))
                .thenThrow(new MunicipioNotFoundException("9999999"));

        mockMvc.perform(get("/api/perfil/9999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("MUNICIPIO_NAO_ENCONTRADO"));
    }

    @Test
    void deveRetornar200ParaMunicipioValido() throws Exception {
        when(consultarPerfilEpidemiologicoUseCase.buscarMunicipio(eq("3550308"), any(), any()))
                .thenReturn(PerfilEpidemiologicoResponse.builder()
                        .coIbge("3550308")
                        .municipio("São Paulo")
                        .classificacao("ALTO")
                        .build());

        mockMvc.perform(get("/api/perfil/3550308"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio").exists())
                .andExpect(jsonPath("$.classificacao").exists());
    }
}
