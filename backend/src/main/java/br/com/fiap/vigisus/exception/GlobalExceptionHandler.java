package br.com.fiap.vigisus.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(NotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 404);
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoNaoEncontradoException(RecursoNaoEncontradoException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 404);
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<Map<String, Object>> handleExternalApiException(ExternalApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 502);
        body.put("error", "Bad Gateway");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(MunicipioNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMunicipioNotFoundException(MunicipioNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("erro", "MUNICIPIO_NAO_ENCONTRADO");
        body.put("mensagem", ex.getMessage());
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DadosInsuficientesException.class)
    public ResponseEntity<Map<String, Object>> handleDadosInsuficientesException(DadosInsuficientesException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("erro", "DADOS_INSUFICIENTES");
        body.put("mensagem", ex.getMessage());
        body.put("sugestao", "Execute o pipeline de ingestão de dados");
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(ApiExternaException.class)
    public ResponseEntity<Map<String, Object>> handleApiExternaException(ApiExternaException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("erro", "SERVICO_EXTERNO_INDISPONIVEL");
        body.put("mensagem", ex.getMessage());
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<Map<String, Object>> handleCompletionException(CompletionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof DadosInsuficientesException dadosEx) {
            return handleDadosInsuficientesException(dadosEx);
        }
        if (cause instanceof MunicipioNotFoundException municipioEx) {
            return handleMunicipioNotFoundException(municipioEx);
        }

        return handleGenericException(ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("erro", "DADOS_INVALIDOS");
        body.put("mensagem", "Requisição inválida: verifique os campos obrigatórios");
        body.put("campos", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList());
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Erro inesperado", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("erro", "ERRO_INTERNO");
        body.put("mensagem", "Erro inesperado. Tente novamente.");
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
