package br.com.fiap.vigisus.exception;

public class ApiExternaException extends RuntimeException {

    public ApiExternaException(String servico) {
        super("Erro ao consultar serviço externo: " + servico);
    }

    public ApiExternaException(String servico, Throwable cause) {
        super("Erro ao consultar serviço externo: " + servico, cause);
    }
}
