package br.com.fiap.vigisus.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleRecursoNaoEncontradoException_retorna404() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleNotFound(new RecursoNaoEncontradoException("Perfil", "9999999"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RECURSO_NAO_ENCONTRADO");
        assertThat(response.getBody().message()).contains("9999999");
    }

    @Test
    void handleMunicipioNotFoundException_retorna404ViaPai() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleNotFound(new MunicipioNotFoundException("123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RECURSO_NAO_ENCONTRADO");
    }

    @Test
    void handleExternalApiException_retorna502() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleExternalApi(new ExternalApiException("Open-Meteo", "resposta nula"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("EXTERNAL_API_ERROR");
        assertThat(response.getBody().message()).contains("Open-Meteo");
    }

    @Test
    void handleDadosInsuficientesException_retorna422() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleDadosInsuficientes(new DadosInsuficientesException("Lavras", 2024));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DADOS_INSUFICIENTES");
    }

    @Test
    void handleCompletionException_delegaParaDadosInsuficientes() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleCompletionException(new CompletionException(new DadosInsuficientesException("Brasil", 2024)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DADOS_INSUFICIENTES");
    }

    @Test
    void handleCompletionException_delegaParaMunicipio() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleCompletionException(new CompletionException(new MunicipioNotFoundException("123")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RECURSO_NAO_ENCONTRADO");
    }

    @Test
    void handleCompletionException_fazFallbackParaGeneric() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleCompletionException(new CompletionException(new RuntimeException("boom")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ERRO_INTERNO");
    }
}
