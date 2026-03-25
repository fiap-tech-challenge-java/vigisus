package br.com.fiap.vigisus.domain.epidemiologia;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComparativoHistoricoEpidemiologicoPolicyTest {

    private final ComparativoHistoricoEpidemiologicoPolicy policy =
            new ComparativoHistoricoEpidemiologicoPolicy();

    @Test
    void gerarComparativo_quandoSemBaseHistorica_retornaMensagemInsuficiente() {
        String comparativo = policy.gerarComparativo(12, 0, 2026);

        assertThat(comparativo).isEqualTo("No mesmo periodo de 2025 nao havia registros comparaveis.");
    }

    @Test
    void gerarComparativo_quandoCrescimento_retornaMensagemDeAumento() {
        String comparativo = policy.gerarComparativo(30, 20, 2026);

        assertThat(comparativo).contains("+10 casos a mais").contains("+50%");
    }

    @Test
    void gerarComparativo_quandoReducao_retornaMensagemDeQueda() {
        String comparativo = policy.gerarComparativo(20, 30, 2026);

        assertThat(comparativo).contains("-10 casos a menos").contains("-33%");
    }

    @Test
    void gerarComparativo_quandoVolumeEquivalente_retornaMensagemSemelhante() {
        String comparativo = policy.gerarComparativo(25, 25, 2026);

        assertThat(comparativo).isEqualTo("Situacao semelhante ao mesmo periodo de 2025.");
    }
}
