package br.com.fiap.vigisus.domain.risco;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificacaoRiscoPolicyTest {

    private final ClassificacaoRiscoMunicipioPolicy classificacaoMunicipio = new ClassificacaoRiscoMunicipioPolicy();
    private final ClassificacaoRiscoAgregadoPolicy classificacaoAgregado = new ClassificacaoRiscoAgregadoPolicy();

    @Test
    void classificaRiscoMunicipalNosLimiaresEsperados() {
        assertThat(classificacaoMunicipio.classificar(1)).isEqualTo("BAIXO");
        assertThat(classificacaoMunicipio.classificar(3)).isEqualTo("MODERADO");
        assertThat(classificacaoMunicipio.classificar(5)).isEqualTo("ALTO");
        assertThat(classificacaoMunicipio.classificar(6)).isEqualTo("MUITO_ALTO");
    }

    @Test
    void classificaRiscoAgregadoNosLimiaresEsperados() {
        assertThat(classificacaoAgregado.classificar(1)).isEqualTo("BAIXO");
        assertThat(classificacaoAgregado.classificar(3)).isEqualTo("MODERADO");
        assertThat(classificacaoAgregado.classificar(5)).isEqualTo("ALTO");
        assertThat(classificacaoAgregado.classificar(6)).isEqualTo("EPIDEMIA");
    }
}
