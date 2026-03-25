package br.com.fiap.vigisus.domain.epidemiologia;

import org.springframework.stereotype.Component;

@Component
public class ComparativoHistoricoEpidemiologicoPolicy {

    public String gerarComparativo(int casosAtual, long casosAnoAnterior, int anoAtual) {
        int anoReferencia = anoAtual - 1;

        if (casosAnoAnterior == 0) {
            return String.format("No mesmo periodo de %d nao havia registros comparaveis.", anoReferencia);
        }

        long diferenca = casosAtual - casosAnoAnterior;
        double variacaoPercentual = (double) diferenca / casosAnoAnterior * 100;

        if (diferenca > 0) {
            return String.format(
                    "Comparando com o mesmo periodo de %d: +%d casos a mais (+%.0f%%).",
                    anoReferencia,
                    diferenca,
                    variacaoPercentual
            );
        }
        if (diferenca < 0) {
            return String.format(
                    "Comparando com o mesmo periodo de %d: %d casos a menos (%.0f%%).",
                    anoReferencia,
                    diferenca,
                    variacaoPercentual
            );
        }
        return String.format("Situacao semelhante ao mesmo periodo de %d.", anoReferencia);
    }
}
