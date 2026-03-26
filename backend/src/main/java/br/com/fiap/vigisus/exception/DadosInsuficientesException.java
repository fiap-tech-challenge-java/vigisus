package br.com.fiap.vigisus.exception;

public class DadosInsuficientesException extends VigisusException {

    public DadosInsuficientesException(String municipio, int ano) {
        super("Dados insuficientes para " + municipio + " no ano " + ano, "DADOS_INSUFICIENTES");
    }
}
