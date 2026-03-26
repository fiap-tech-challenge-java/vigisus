package br.com.fiap.vigisus.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionConstructorsTest {

    @Test
    void externalApiException_montaMensagemPadrao() {
        ExternalApiException ex = new ExternalApiException("Open-Meteo", "resposta nula");

        assertThat(ex).hasMessageContaining("Open-Meteo");
        assertThat(ex.getErrorCode()).isEqualTo("EXTERNAL_API_ERROR");
    }

    @Test
    void recursoNaoEncontradoException_montaMensagemPadrao() {
        RecursoNaoEncontradoException ex = new RecursoNaoEncontradoException("Município", "123");

        assertThat(ex).hasMessageContaining("Município").hasMessageContaining("123");
        assertThat(ex.getErrorCode()).isEqualTo("RECURSO_NAO_ENCONTRADO");
    }

    @Test
    void municipioNotFoundException_estendeRecursoNaoEncontrado() {
        MunicipioNotFoundException ex = new MunicipioNotFoundException("3106200");

        assertThat(ex).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(ex.getErrorCode()).isEqualTo("RECURSO_NAO_ENCONTRADO");
        assertThat(ex).hasMessageContaining("3106200");
    }
}
