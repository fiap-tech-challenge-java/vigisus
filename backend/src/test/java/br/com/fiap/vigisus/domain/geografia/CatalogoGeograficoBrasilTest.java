package br.com.fiap.vigisus.domain.geografia;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoGeograficoBrasilTest {

    private final CatalogoGeograficoBrasil catalogo = new CatalogoGeograficoBrasil();

    @Test
    void nomeEstado_retornaNomeMapeado() {
        assertThat(catalogo.nomeEstado("MG")).isEqualTo("Minas Gerais");
    }

    @Test
    void nomeEstado_retornaUfOriginalQuandoDesconhecida() {
        assertThat(catalogo.nomeEstado("XX")).isEqualTo("XX");
    }

    @Test
    void centroideEstado_retornaCopiaDoCentroideMapeado() {
        double[] centroide = catalogo.centroideEstado("SP");

        assertThat(centroide).containsExactly(-22.1875, -48.7966);

        centroide[0] = 0.0;

        assertThat(catalogo.centroideEstado("SP")).containsExactly(-22.1875, -48.7966);
    }

    @Test
    void centroideEstado_retornaCentroidePadraoQuandoUfNula() {
        assertThat(catalogo.centroideEstado(null)).containsExactly(-15.7801, -47.9292);
    }

    @Test
    void codigoCapital_preservaMapasDiferentesPorContexto() {
        assertThat(catalogo.codigoCapitalRiscoAgregado("AC")).isEqualTo("1100015");
        assertThat(catalogo.codigoCapitalEncaminhamento("AC")).isEqualTo("1200401");
        assertThat(catalogo.codigoCapitalRiscoAgregado("MG")).isEqualTo("3106200");
        assertThat(catalogo.codigoCapitalEncaminhamento("MG")).isEqualTo("3106200");
    }
}
