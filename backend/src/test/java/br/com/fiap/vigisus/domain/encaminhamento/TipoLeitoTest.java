package br.com.fiap.vigisus.domain.encaminhamento;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TipoLeitoTest {

    @Test
    void porGravidade_mapeiaValoresEsperados() {
        assertThat(TipoLeito.porGravidade("grave").codigo()).isEqualTo("81");
        assertThat(TipoLeito.porGravidade("critica").codigo()).isEqualTo("81");
        assertThat(TipoLeito.porGravidade("moderada").codigo()).isEqualTo("74");
        assertThat(TipoLeito.porGravidade(null).codigo()).isEqualTo("74");
    }

    @Test
    void of_quandoCodigoValido_identificaTipo() {
        TipoLeito clinico = TipoLeito.of("74");
        TipoLeito uti = TipoLeito.of("81");

        assertThat(clinico.ehClinico()).isTrue();
        assertThat(clinico.ehUti()).isFalse();
        assertThat(uti.ehUti()).isTrue();
        assertThat(uti.ehClinico()).isFalse();
    }

    @Test
    void of_quandoVazio_lancaErro() {
        assertThatThrownBy(() -> TipoLeito.of(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leito");
    }
}
