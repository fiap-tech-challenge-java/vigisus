package br.com.fiap.vigisus.exception;

public class ExternalApiException extends VigisusException {

    public ExternalApiException(String servico, String motivo) {
        super("Falha na API externa [" + servico + "]: " + motivo, "EXTERNAL_API_ERROR");
    }
}
