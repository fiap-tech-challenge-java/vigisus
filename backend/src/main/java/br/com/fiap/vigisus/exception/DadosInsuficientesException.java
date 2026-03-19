package br.com.fiap.vigisus.exception;

public class DadosInsuficientesException extends RuntimeException {

    public DadosInsuficientesException(String municipio, int ano) {
        super("Dados insuficientes para " + municipio + " no ano " + ano);
    }
}
