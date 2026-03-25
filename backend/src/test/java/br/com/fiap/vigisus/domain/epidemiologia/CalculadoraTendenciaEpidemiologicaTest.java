package br.com.fiap.vigisus.domain.epidemiologia;

import br.com.fiap.vigisus.dto.SemanaDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraTendenciaEpidemiologicaTest {

    private final CalculadoraTendenciaEpidemiologica calculadora = new CalculadoraTendenciaEpidemiologica();

    @Test
    void retornaEstavelQuandoNaoHaSemanasSuficientes() {
        assertThat(calculadora.calcular(List.of(
                semana(1, 10),
                semana(2, 20)
        ))).isEqualTo("ESTAVEL");
    }

    @Test
    void retornaCrescenteQuandoAsUltimasQuatroSemanasCrescemMaisDeVintePorCento() {
        assertThat(calculadora.calcular(List.of(
                semana(1, 10), semana(2, 10), semana(3, 10), semana(4, 10),
                semana(5, 20), semana(6, 20), semana(7, 20), semana(8, 20)
        ))).isEqualTo("CRESCENTE");
    }

    @Test
    void retornaDecrescenteQuandoAsUltimasQuatroSemanasCaemMaisDeVintePorCento() {
        assertThat(calculadora.calcular(List.of(
                semana(1, 20), semana(2, 20), semana(3, 20), semana(4, 20),
                semana(5, 10), semana(6, 10), semana(7, 10), semana(8, 10)
        ))).isEqualTo("DECRESCENTE");
    }

    @Test
    void retornaEstavelQuandoVariacaoFicaDentroDaFaixaNeutra() {
        assertThat(calculadora.calcular(List.of(
                semana(1, 10), semana(2, 10), semana(3, 10), semana(4, 10),
                semana(5, 11), semana(6, 10), semana(7, 10), semana(8, 10)
        ))).isEqualTo("ESTAVEL");
    }

    private SemanaDTO semana(int numero, int casos) {
        return SemanaDTO.builder()
                .semanaEpi(numero)
                .casos(casos)
                .build();
    }
}
