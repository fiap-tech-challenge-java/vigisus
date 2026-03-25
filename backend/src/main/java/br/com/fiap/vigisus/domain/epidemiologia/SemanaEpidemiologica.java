package br.com.fiap.vigisus.domain.epidemiologia;

public record SemanaEpidemiologica(int ano, int numero) {

    public SemanaEpidemiologica {
        if (ano <= 0) {
            throw new IllegalArgumentException("Ano epidemiologico invalido");
        }
        if (numero <= 0 || numero > 53) {
            throw new IllegalArgumentException("Semana epidemiologica invalida");
        }
    }
}
