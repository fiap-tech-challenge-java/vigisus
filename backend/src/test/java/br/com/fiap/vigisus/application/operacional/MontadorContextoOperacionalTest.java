package br.com.fiap.vigisus.application.operacional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MontadorContextoOperacionalTest {

    private final MontadorContextoOperacional montador = new MontadorContextoOperacional();

    @Test
    void montarContextoAtual_montaMensagemComParametros() {
        String contexto = montador.montarContextoAtual(7, "Lavras", "ALTO", "CRESCENTE");

        assertThat(contexto).contains("7 suspeitas").contains("Lavras").contains("ALTO").contains("CRESCENTE");
    }

    @Test
    void montarPadraoHistorico_quandoSemDados_retornaFallback() {
        String padrao = montador.montarPadraoHistorico("No mesmo perÃƒÂ­odo de 2025 nÃƒÂ£o havia registros comparÃƒÂ¡veis.");

        assertThat(padrao).contains("insuficientes");
    }

    @Test
    void montarPadraoHistorico_quandoComparativoValido_retornaTextoOriginal() {
        String comparativo = "Comparando com o mesmo perÃƒÂ­odo de 2025: +10 casos a mais (+20%).";

        assertThat(montador.montarPadraoHistorico(comparativo)).isEqualTo(comparativo);
    }

    @Test
    void montarContextoIa_compactaCamposParaPrompt() {
        String contextoIa = montador.montarContextoIa("contexto", "padrao", "ELEVADO");

        assertThat(contextoIa).contains("Contexto atual: contexto")
                .contains("PadrÃƒÂ£o histÃƒÂ³rico: padrao")
                .contains("NÃƒÂ­vel de atenÃƒÂ§ÃƒÂ£o: ELEVADO");
    }
}
