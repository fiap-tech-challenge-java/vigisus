package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.PerfilEpidemiologicoService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerfilControllerTest {

    @Test
    void getPerfil_aplicaTextoIa() {
        PerfilEpidemiologicoService perfilService = mock(PerfilEpidemiologicoService.class);
        IaService iaService = mock(IaService.class);
        PerfilController controller = new PerfilController(perfilService, iaService);

        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder().municipio("Lavras").build();
        when(perfilService.gerarPerfil("3131307", "dengue", LocalDate.now().getYear())).thenReturn(perfil);
        when(iaService.gerarTextoEpidemiologico(perfil)).thenReturn("analise");

        PerfilEpidemiologicoResponse response = controller.getPerfil("3131307", "dengue", null);

        assertThat(response.getTextoIa()).isEqualTo("analise");
    }
}
