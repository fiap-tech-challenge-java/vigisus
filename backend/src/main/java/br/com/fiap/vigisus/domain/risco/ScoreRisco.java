package br.com.fiap.vigisus.domain.risco;

/**
 * Score de risco epidemiológico-climático.
 * Value Object imutável — range 0-100.
 */
public record ScoreRisco(int valor) {

    public ScoreRisco {
        if (valor < 0 || valor > 100) {
            throw new IllegalArgumentException(
                "Score de risco deve estar entre 0 e 100: " + valor);
        }
    }

    public boolean isBaixo()    { return valor < 30; }
    public boolean isModerado() { return valor >= 30 && valor < 60; }
    public boolean isAlto()     { return valor >= 60 && valor < 80; }
    public boolean isMuitoAlto(){ return valor >= 80; }

    public String classificacao() {
        if (isMuitoAlto()) return "MUITO_ALTO";
        if (isAlto())      return "ALTO";
        if (isModerado())  return "MODERADO";
        return "BAIXO";
    }
}
