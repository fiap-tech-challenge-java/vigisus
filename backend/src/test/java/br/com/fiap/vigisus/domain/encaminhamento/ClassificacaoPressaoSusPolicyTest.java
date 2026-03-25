package br.com.fiap.vigisus.domain.encaminhamento;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificacaoPressaoSusPolicyTest {

    private final ClassificacaoPressaoSusPolicy policy = new ClassificacaoPressaoSusPolicy();

    @ParameterizedTest
    @CsvSource({
            "0,10,NORMAL",
            "100,20,NORMAL",
            "250,20,ELEVADA",
            "300,20,ALTA",
            "400,20,CRITICA",
            "100,0,NORMAL"
    })
    void classificar_retornaFaixaEsperada(long totalRecente, int leitosSus, String esperado) {
        assertThat(policy.classificar(totalRecente, leitosSus)).isEqualTo(esperado);
    }
}
