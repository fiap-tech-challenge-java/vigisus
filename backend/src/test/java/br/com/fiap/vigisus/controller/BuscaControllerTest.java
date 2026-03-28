package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.busca.BuscaCompletaUseCase;
import br.com.fiap.vigisus.dto.BuscaCompletaResponse;
import br.com.fiap.vigisus.dto.BuscaRequest;
import br.com.fiap.vigisus.exception.RecursoNaoEncontradoException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscaControllerTest {

    private BuscaCompletaUseCase buscaCompletaUseCase;
    private BuscaController controller;

    @BeforeEach
    void setUp() {
        buscaCompletaUseCase = mock(BuscaCompletaUseCase.class);
        controller = new BuscaController(buscaCompletaUseCase, new SimpleMeterRegistry());
    }

    @Test
    void buscar_delegaParaUseCase() {
        BuscaCompletaResponse resposta = BuscaCompletaResponse.builder().textoIa("briefing").build();
        when(buscaCompletaUseCase.buscarPorPergunta("dengue em Lavras MG 2024")).thenReturn(resposta);

        BuscaCompletaResponse response = controller.buscar(new BuscaRequest("dengue em Lavras MG 2024"));

        assertThat(response.getTextoIa()).isEqualTo("briefing");
    }

    @Test
    void buscarDireto_delegaParaUseCase() {
        BuscaCompletaResponse resposta = BuscaCompletaResponse.builder().textoIa("direto").build();
        when(buscaCompletaUseCase.buscarDireto("Lavras", "MG", "dengue", 2024)).thenReturn(resposta);

        BuscaCompletaResponse response = controller.buscarDireto("Lavras", "MG", "dengue", 2024).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getTextoIa()).isEqualTo("direto");
    }

    @Test
    void buscar_propagaErrosDoUseCase() {
        when(buscaCompletaUseCase.buscarPorPergunta("pergunta vaga"))
                .thenThrow(new RecursoNaoEncontradoException("Município", "nome não fornecido na pergunta"));

        assertThatThrownBy(() -> controller.buscar(new BuscaRequest("pergunta vaga")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Município");
    }
}
