package br.com.fiap.vigisus.application.operacional;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarProtocoloSurtoUseCaseTest {

    @Test
    void executar_retornaEstruturaEsperada() {
        ConsultarProtocoloSurtoUseCase useCase = new ConsultarProtocoloSurtoUseCase();

        Map<String, Object> response = useCase.executar();

        assertThat(response).containsKey("titulo");
        assertThat((List<?>) response.get("passos")).isNotEmpty();
        assertThat(((Map<?, ?>) response.get("contatos")).containsKey("vigilancia_epidemiologica")).isTrue();
    }
}
