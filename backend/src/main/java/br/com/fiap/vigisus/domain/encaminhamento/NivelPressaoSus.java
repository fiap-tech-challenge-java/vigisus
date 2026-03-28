package br.com.fiap.vigisus.domain.encaminhamento;

/**
 * Nível de pressão assistencial do SUS.
 * Value Object imutável baseado em percentual de ocupação.
 */
public record NivelPressaoSus(int percentualOcupacao) {

    public NivelPressaoSus {
        if (percentualOcupacao < 0 || percentualOcupacao > 100) {
            throw new IllegalArgumentException(
                "Percentual de ocupação inválido: " + percentualOcupacao);
        }
    }

    public boolean isNormal()  { return percentualOcupacao < 80; }
    public boolean isAlerta()  { return percentualOcupacao >= 80 && percentualOcupacao < 90; }
    public boolean isCritico() { return percentualOcupacao >= 90; }

    public String descricao() {
        if (isCritico()) return "CRÍTICO";
        if (isAlerta())  return "ALERTA";
        return "NORMAL";
    }
}
