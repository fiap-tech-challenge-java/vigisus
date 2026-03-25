package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.SemanaDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextoAnaliticoHelperTest {

    @Test
    void montarTextoEpidemiologico_incluiComparacoesEPicoQuandoDadosPresentes() {
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .total(120)
                .incidencia(130.5)
                .classificacao("ALTO")
                .tendencia("CRESCENTE")
                .posicaoEstado("ocupou a 3a posicao no estado")
                .incidenciaMediaEstado(80.2)
                .semanas(List.of(
                        SemanaDTO.builder().semanaEpi(10).casos(30).build(),
                        SemanaDTO.builder().semanaEpi(11).casos(50).build()
                ))
                .semanasAnoAnterior(List.of(
                        SemanaDTO.builder().semanaEpi(10).casos(20).build(),
                        SemanaDTO.builder().semanaEpi(11).casos(40).build()
                ))
                .build();

        String texto = TextoAnaliticoHelper.montarTextoEpidemiologico(perfil);

        assertThat(texto).contains("Lavras/MG");
        assertThat(texto).contains("120 casos de dengue em 2024");
        assertThat(texto).contains("comparacao com 2023");
        assertThat(texto).contains("semana 11");
        assertThat(texto).contains("tendencia das semanas mais recentes foi classificada como crescente");
        assertThat(texto).contains("ocupou a 3a posicao no estado");
        assertThat(texto).contains("80.2 por 100 mil");
    }

    @Test
    void montarTextoEpidemiologico_aplicaFallbacksEOmiteSecoesOpcionais() {
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .municipio(" ")
                .uf(null)
                .doenca(null)
                .ano(2024)
                .total(0)
                .incidencia(0.0)
                .classificacao(null)
                .semanas(List.of())
                .semanasAnoAnterior(List.of())
                .build();

        String texto = TextoAnaliticoHelper.montarTextoEpidemiologico(perfil);

        assertThat(texto).contains("Local/BR");
        assertThat(texto).contains("0 casos de dengue em 2024");
        assertThat(texto).contains("classificacao sem_dado");
        assertThat(texto).doesNotContain("comparacao com 2023");
        assertThat(texto).doesNotContain("No recorte estadual");
    }

    @Test
    void montarTextoRisco_limitaFatoresETemFallbackSemLista() {
        String textoComFatores = TextoAnaliticoHelper.montarTextoRisco(PrevisaoRiscoResponse.builder()
                .municipio("Lavras")
                .score(6)
                .classificacao("ALTO")
                .fatores(List.of("chuva", "temperatura", "umidade", "vento"))
                .build());

        String textoSemFatores = TextoAnaliticoHelper.montarTextoRisco(PrevisaoRiscoResponse.builder()
                .municipio(null)
                .score(0)
                .classificacao(null)
                .fatores(List.of())
                .build());

        assertThat(textoComFatores).contains("Lavras apresenta risco alto");
        assertThat(textoComFatores).contains("chuva; temperatura; umidade");
        assertThat(textoComFatores).doesNotContain("vento");
        assertThat(textoSemFatores).contains("O territorio apresenta risco sem_dado");
        assertThat(textoSemFatores).contains("fatores climaticos nao detalhados");
    }

    @Test
    void montarTextoOperacionalESomarCasos_funcionamComoEsperado() {
        String texto = TextoAnaliticoHelper.montarTextoOperacional("cenario de pressao");

        assertThat(texto).startsWith("Leitura operacional consolidada: cenario de pressao");
        assertThat(TextoAnaliticoHelper.somarCasos(null)).isZero();
        assertThat(TextoAnaliticoHelper.somarCasos(List.of(
                SemanaDTO.builder().semanaEpi(1).casos(2).build(),
                SemanaDTO.builder().semanaEpi(2).casos(3).build()
        ))).isEqualTo(5);
    }
}
