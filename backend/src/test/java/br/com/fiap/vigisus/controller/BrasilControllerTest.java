package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.SemanaDTO;
import br.com.fiap.vigisus.service.BrasilEpidemiologicoService;
import br.com.fiap.vigisus.service.IaService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrasilControllerTest {

    @Test
    void getCasosBrasil_aplicaAnoPadraoETextoIa() {
        BrasilEpidemiologicoService brasilService = mock(BrasilEpidemiologicoService.class);
        IaService iaService = mock(IaService.class);
        BrasilController controller = new BrasilController(brasilService, iaService);

        BrasilEpidemiologicoResponse perfil = BrasilEpidemiologicoResponse.builder()
                .totalCasos(100L)
                .incidencia(55.0)
                .classificacao("MODERADO")
                .tendencia("ESTAVEL")
                .semanas(List.of(SemanaDTO.builder().semanaEpi(1).casos(10).build()))
                .semanasAnoAnterior(List.of())
                .build();
        when(brasilService.gerarPerfilBrasil("dengue", LocalDate.now().getYear())).thenReturn(perfil);
        when(iaService.gerarTextoEpidemiologico(any())).thenReturn("texto");

        BrasilEpidemiologicoResponse response = controller.getCasosBrasil("dengue", null);

        assertThat(response.getTextoIa()).isEqualTo("texto");
    }
}
