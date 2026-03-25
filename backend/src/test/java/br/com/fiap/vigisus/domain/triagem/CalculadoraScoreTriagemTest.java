package br.com.fiap.vigisus.domain.triagem;

import br.com.fiap.vigisus.dto.TriagemRequest;
import org.junit.jupiter.api.Test;

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
}
