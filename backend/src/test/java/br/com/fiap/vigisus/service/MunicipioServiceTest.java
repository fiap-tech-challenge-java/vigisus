package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.exception.MunicipioNotFoundException;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MunicipioServiceTest {

    @Mock
    private MunicipioRepository municipioRepository;

    private MunicipioService service;

    @BeforeEach
    void setUp() {
        service = new MunicipioService(municipioRepository);
    }

    @Test
    void buscarPorCoIbge_quandoEncontrado_retornaMunicipio() {
        Municipio municipio = Municipio.builder()
                .coIbge("3131307")
                .noMunicipio("Lavras")
                .sgUf("MG")
                .build();
        when(municipioRepository.findByCoIbge("3131307")).thenReturn(Optional.of(municipio));

        Municipio result = service.buscarPorCoIbge("3131307");

        assertThat(result.getNoMunicipio()).isEqualTo("Lavras");
    }

    @Test
    void buscarPorCoIbge_quandoNaoEncontrado_lancaMunicipioNotFoundException() {
        when(municipioRepository.findByCoIbge("9999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorCoIbge("9999999"))
                .isInstanceOf(MunicipioNotFoundException.class)
                .hasMessage("Município não encontrado: 9999999");
    }
}
