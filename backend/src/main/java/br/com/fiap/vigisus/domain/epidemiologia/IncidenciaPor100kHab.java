package br.com.fiap.vigisus.domain.epidemiologia;

/**
 * Incidência epidemiológica por 100 mil habitantes.
 * Value Object imutável — não pode ser negativo.
 */
public record IncidenciaPor100kHab(double valor) {

    public IncidenciaPor100kHab {
        if (valor < 0) {
            throw new IllegalArgumentException(
                "Incidência não pode ser negativa: " + valor);
        }
    }

    public static IncidenciaPor100kHab calcular(long casos, long populacao) {
        if (populacao == 0) return new IncidenciaPor100kHab(0.0);
        return new IncidenciaPor100kHab((casos * 100_000.0) / populacao);
    }

    public boolean isBaixa()    { return valor < 100.0; }
    public boolean isMedia()    { return valor >= 100.0 && valor < 300.0; }
    public boolean isAlta()     { return valor >= 300.0 && valor < 1000.0; }
    public boolean isMuitoAlta(){ return valor >= 1000.0; }

    @Override
    public String toString() {
        return String.format("%.1f/100k hab", valor);
    }
}
