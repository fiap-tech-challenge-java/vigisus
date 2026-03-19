package br.com.fiap.vigisus.exception;

public class MunicipioNotFoundException extends RuntimeException {

    public MunicipioNotFoundException(String coIbge) {
        super("Município não encontrado: " + coIbge);
    }
}
