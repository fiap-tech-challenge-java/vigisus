package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.operacional.AvaliarPressaoOperacionalUseCase;
import br.com.fiap.vigisus.application.operacional.ConsultarProtocoloSurtoUseCase;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PressaoOperacionalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AvaliarPressaoOperacionalUseCase avaliarPressaoOperacionalUseCase;

    @MockBean
    private ConsultarProtocoloSurtoUseCase consultarProtocoloSurtoUseCase;

    @Test
    void deveRetornar200ParaPressaoValida() throws Exception {
        String requestBody = """
                {
                  "municipio": "Campinas",
                  "suspeitasDengueDia": 15,
                  "tipoUnidade": "UBS"
                }
                """;

        when(avaliarPressaoOperacionalUseCase.executar(any()))
                .thenReturn(PressaoOperacionalResponse.builder().nivelAtencao("MODERADO").build());

        mockMvc.perform(post("/api/operacional/pressao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar400ParaMunicipioVazio() throws Exception {
        String requestBody = """
                {
                  "municipio": "",
                  "suspeitasDengueDia": 15,
                  "tipoUnidade": "UBS"
                }
                """;

        mockMvc.perform(post("/api/operacional/pressao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos").isArray());
    }

    @Test
    void deveRetornar400ParaTipoUnidadeVazio() throws Exception {
        String requestBody = """
                {
                  "municipio": "Campinas",
                  "suspeitasDengueDia": 10,
                  "tipoUnidade": ""
                }
                """;

        mockMvc.perform(post("/api/operacional/pressao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos").isArray());
    }
}
