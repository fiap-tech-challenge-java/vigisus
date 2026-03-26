package br.com.fiap.vigisus.exception;

public class MunicipioNotFoundException extends RecursoNaoEncontradoException {

    public MunicipioNotFoundException(String coIbge) {
        super("Município", coIbge);
    }
}
