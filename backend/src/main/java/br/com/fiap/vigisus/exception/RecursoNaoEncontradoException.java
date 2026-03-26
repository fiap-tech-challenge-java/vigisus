package br.com.fiap.vigisus.exception;

public class RecursoNaoEncontradoException extends VigisusException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " não encontrado(a): " + id, "RECURSO_NAO_ENCONTRADO");
    }
}
