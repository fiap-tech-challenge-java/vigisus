package br.com.fiap.vigisus.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFoundException_retorna404() {
        ResponseEntity<Map<String, Object>> response = handler.handleNotFoundException(new NotFoundException("nao achou"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "nao achou");
    }

    @Test
    void handleRecursoNaoEncontradoException_retorna404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRecursoNaoEncontradoException(new RecursoNaoEncontradoException("faltou"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Not Found");
    }

    @Test
    void handleExternalApiException_retorna502() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleExternalApiException(new ExternalApiException("falha externa"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("message", "falha externa");
    }

    @Test
    void handleMunicipioNotFoundException_retorna404Customizado() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMunicipioNotFoundException(new MunicipioNotFoundException("123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("erro", "MUNICIPIO_NAO_ENCONTRADO");
    }

    @Test
    void handleDadosInsuficientesException_retorna422() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleDadosInsuficientesException(new DadosInsuficientesException("Lavras", 2024));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsEntry("erro", "DADOS_INSUFICIENTES");
    }

    @Test
    void handleApiExternaException_retorna502Customizado() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleApiExternaException(new ApiExternaException("Open-Meteo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("erro", "SERVICO_EXTERNO_INDISPONIVEL");
    }

    @Test
    void handleCompletionException_delegaParaDadosInsuficientes() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleCompletionException(new CompletionException(new DadosInsuficientesException("Brasil", 2024)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsEntry("erro", "DADOS_INSUFICIENTES");
    }

    @Test
    void handleCompletionException_delegaParaMunicipio() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleCompletionException(new CompletionException(new MunicipioNotFoundException("123")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("erro", "MUNICIPIO_NAO_ENCONTRADO");
    }

    @Test
    void handleCompletionException_fazFallbackParaGeneric() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleCompletionException(new CompletionException(new RuntimeException("boom")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("erro", "ERRO_INTERNO");
    }
}
