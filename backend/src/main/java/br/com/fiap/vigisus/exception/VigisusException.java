package br.com.fiap.vigisus.exception;

public abstract class VigisusException extends RuntimeException {

    private final String errorCode;

    protected VigisusException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
