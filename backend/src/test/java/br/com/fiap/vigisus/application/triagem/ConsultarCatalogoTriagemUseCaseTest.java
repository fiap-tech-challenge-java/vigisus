package br.com.fiap.vigisus.application.triagem;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarCatalogoTriagemUseCaseTest {

    @Test
    void executar_retornaCatalogosFixos() {
        ConsultarCatalogoTriagemUseCase useCase = new ConsultarCatalogoTriagemUseCase();

        Map<String, List<String>> resposta = useCase.executar();

        assertThat(resposta).containsKeys("sintomas", "comorbidades");
        assertThat(resposta.get("sintomas")).contains("febre");
        assertThat(resposta.get("comorbidades")).contains("diabetes");
    }
}
