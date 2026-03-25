package br.com.fiap.vigisus.domain.triagem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PriorizacaoTriagemPolicyTest {

    private final PriorizacaoTriagemPolicy policy = new PriorizacaoTriagemPolicy();

    @ParameterizedTest
    @CsvSource({
            "EPIDEMIA,1.5",
            "ALTO,1.2",
            "BAIXO,0.8",
            "MODERADO,1.0"
    })
    void resolverMultiplicador_retornaValorEsperado(String classificacao, double esperado) {
        assertThat(policy.resolverMultiplicador(classificacao)).isEqualTo(esperado);
    }

    @Test
    void resolverMultiplicador_quandoNulo_retornaNeutro() {
        assertThat(policy.resolverMultiplicador(null)).isEqualTo(1.0);
    }

    @ParameterizedTest
    @CsvSource({
            "0.0,BAIXA,VERDE,false",
            "3.0,MEDIA,AMARELO,false",
            "5.01,ALTA,LARANJA,true",
            "8.01,CRITICA,VERMELHO,true"
    })
    void classificarPrioridadeResolveCorEObservacao(double scoreFinal, String prioridade,
                                                     String cor, boolean requerObservacao) {
        String resultado = policy.classificarPrioridade(scoreFinal);

        assertThat(resultado).isEqualTo(prioridade);
        assertThat(policy.resolverCor(resultado)).isEqualTo(cor);
        assertThat(policy.requerObservacao(resultado)).isEqualTo(requerObservacao);
    }
}
