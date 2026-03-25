package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarBrasilEpidemiologicoUseCase;
import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrasilControllerTest {

    @Test
    void getCasosBrasil_delegaParaUseCase() {
        ConsultarBrasilEpidemiologicoUseCase useCase = mock(ConsultarBrasilEpidemiologicoUseCase.class);
        BrasilController controller = new BrasilController(useCase);
        BrasilEpidemiologicoResponse perfil = BrasilEpidemiologicoResponse.builder().textoIa("texto").build();

        when(useCase.buscar("dengue", null)).thenReturn(perfil);

        BrasilEpidemiologicoResponse response = controller.getCasosBrasil("dengue", null);

        assertThat(response.getTextoIa()).isEqualTo("texto");
    }
}
