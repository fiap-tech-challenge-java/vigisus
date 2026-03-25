package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IaServiceFallbackTest {

    private IaServiceFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new IaServiceFallback();
    }

    @Test
    void gerarTextoEpidemiologico_retornaTextoFormatado() {
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .municipio("Sao Paulo")
                .uf("SP")
                .doenca("dengue")
                .ano(2024)
                .total(1500L)
                .incidencia(125.3)
                .classificacao("ALTO")
                .build();

        String texto = fallback.gerarTextoEpidemiologico(perfil);

        assertThat(texto).contains("2024");
        assertThat(texto).contains("Sao Paulo");
        assertThat(texto).contains("SP");
        assertThat(texto).contains("1500");
        assertThat(texto).contains("dengue");
        assertThat(texto).contains("classificacao alto");
    }

    @Test
    void gerarTextoRisco_retornaTextoComFatores() {
        PrevisaoRiscoResponse previsao = PrevisaoRiscoResponse.builder()
                .municipio("Campinas")
                .score(6)
                .classificacao("MUITO_ALTO")
                .fatores(List.of("Temperatura >= 28C", "Chuva >= 100mm"))
                .build();

        String texto = fallback.gerarTextoRisco(previsao);

        assertThat(texto).contains("Campinas");
        assertThat(texto).contains("risco muito_alto");
        assertThat(texto).contains("Temperatura >= 28C");
        assertThat(texto).contains("Chuva >= 100mm");
    }

    @Test
    void gerarTextoRisco_comFatoresVazios_usaTextoPadrao() {
        PrevisaoRiscoResponse previsao = PrevisaoRiscoResponse.builder()
                .municipio("Curitiba")
                .score(0)
                .classificacao("BAIXO")
                .fatores(List.of())
                .build();

        String texto = fallback.gerarTextoRisco(previsao);

        assertThat(texto).contains("fatores climaticos nao detalhados");
    }

    @Test
    void interpretarPergunta_extraiAno() {
        IntencaoDTO intencao = fallback.interpretarPergunta("Casos de dengue em 2023 em SP");

        assertThat(intencao.getAno()).isEqualTo(2023);
    }

    @Test
    void interpretarPergunta_extraiDoenca() {
        IntencaoDTO intencao = fallback.interpretarPergunta("quantos casos de chikungunya");

        assertThat(intencao.getDoenca()).isEqualToIgnoringCase("chikungunya");
    }

    @Test
    void interpretarPergunta_extraiUf() {
        IntencaoDTO intencao = fallback.interpretarPergunta("dengue em RJ 2024");

        assertThat(intencao.getUf()).isEqualTo("RJ");
    }

    @Test
    void interpretarPergunta_semAno_usaAnoAtual() {
        IntencaoDTO intencao = fallback.interpretarPergunta("dengue em SP");

        assertThat(intencao.getAno()).isEqualTo(Year.now().getValue());
    }
}
