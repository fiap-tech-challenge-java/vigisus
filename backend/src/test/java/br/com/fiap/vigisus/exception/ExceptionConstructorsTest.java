package br.com.fiap.vigisus.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionConstructorsTest {

    @Test
    void externalApiException_preservaMensagemECausa() {
        RuntimeException cause = new RuntimeException("boom");

        ExternalApiException semCausa = new ExternalApiException("falha externa");
        ExternalApiException comCausa = new ExternalApiException("falha externa", cause);

        assertThat(semCausa).hasMessage("falha externa").hasNoCause();
        assertThat(comCausa).hasMessage("falha externa").hasCause(cause);
    }

    @Test
    void apiExternaException_montaMensagemPadrao() {
        RuntimeException cause = new RuntimeException("boom");

        ApiExternaException semCausa = new ApiExternaException("OpenMeteo");
        ApiExternaException comCausa = new ApiExternaException("OpenMeteo", cause);

        assertThat(semCausa).hasMessageContaining("OpenMeteo").hasNoCause();
        assertThat(comCausa).hasMessageContaining("OpenMeteo").hasCause(cause);
    }
}
