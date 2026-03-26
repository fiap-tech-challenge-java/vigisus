package br.com.fiap.vigisus.domain.triagem;

import br.com.fiap.vigisus.dto.TriagemRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraScoreTriagemTest {

    private final CalculadoraScoreTriagem calculadora = new CalculadoraScoreTriagem();

    @Test
    void calcular_consideraSintomasComorbidadesIdadeEDias() {
        double score = calculadora.calcular(TriagemRequest.builder()
                .municipio("3131307")
                .sintomas(List.of("falta_ar", "febre", "tontura"))
                .comorbidades(List.of("diabetes", "hipertensao"))
                .diasSintomas(5)
                .idade(70)
                .build());

        assertThat(score).isEqualTo(11.0);
    }

    @Test
    void calcular_quandoListasNulas_trataComoVazias() {
        double score = calculadora.calcular(TriagemRequest.builder()
                .municipio("3131307")
                .sintomas(null)
                .comorbidades(null)
                .diasSintomas(1)
                .idade(30)
                .build());

        assertThat(score).isZero();
    }

    @Test
    void calcular_quandoIdadeMenorQue2_adicionaPontuacaoDeRisco() {
        double score = calculadora.calcular(TriagemRequest.builder()
                .municipio("3131307")
                .sintomas(List.of())
                .comorbidades(List.of())
                .diasSintomas(1)
                .idade(1)
                .build());

        assertThat(score).isEqualTo(2.0);
    }

    @Test
    void calcular_quandoSomenteComorbidadesGraves_pontuaCorretamente() {
        double score = calculadora.calcular(TriagemRequest.builder()
                .municipio("3131307")
                .sintomas(List.of())
                .comorbidades(List.of("gestante", "doenca_renal", "obesidade"))
                .diasSintomas(1)
                .idade(30)
                .build());

        assertThat(score).isEqualTo(5.0);
    }

    @Test
    void calcular_quandoDiasSintomasMenorQue5_naoAdicionaPonto() {
        double score = calculadora.calcular(TriagemRequest.builder()
                .municipio("3131307")
                .sintomas(List.of())
                .comorbidades(List.of())
                .diasSintomas(4)
                .idade(30)
                .build());

        assertThat(score).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "dor_abdominal, 3.0",
            "sangramento,   3.0",
            "vomito,        3.0",
            "prostacao,     3.0",
            "febre,         1.0",
            "exantema,      1.0",
            "nausea,        1.0"
    })
    void calcular_pontuaCadaSintomaIndividualmente(String sintoma, double pontuacaoEsperada) {
        double score = calculadora.calcular(TriagemRequest.builder()
                .municipio("3131307")
                .sintomas(List.of(sintoma))
                .comorbidades(List.of())
                .diasSintomas(1)
                .idade(30)
                .build());

        assertThat(score).isEqualTo(pontuacaoEsperada);
    }
}
