package br.com.fiap.vigisus.domain.geografia;

public record CoIbge(String value) {

    public CoIbge {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("coIbge nao pode ser vazio");
        }
        value = value.trim();
    }

    public static CoIbge of(String value) {
        return new CoIbge(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
