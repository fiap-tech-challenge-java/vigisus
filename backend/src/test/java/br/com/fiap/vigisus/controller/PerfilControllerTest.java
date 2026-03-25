package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarPerfilEpidemiologicoUseCase;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerfilControllerTest {

    @Test
    void getPerfil_delegaParaUseCase() {
        ConsultarPerfilEpidemiologicoUseCase useCase = mock(ConsultarPerfilEpidemiologicoUseCase.class);
        PerfilController controller = new PerfilController(useCase);

        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder().textoIa("analise").build();
        when(useCase.buscarMunicipio("3131307", "dengue", null)).thenReturn(perfil);

        PerfilEpidemiologicoResponse response = controller.getPerfil("3131307", "dengue", null);

        assertThat(response.getTextoIa()).isEqualTo("analise");
    }
}
