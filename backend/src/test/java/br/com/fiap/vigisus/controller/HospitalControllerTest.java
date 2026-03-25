package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HospitalControllerTest {

    @Test
    void getHospitais_retornaListaDoService() {
        EncaminhamentoService service = mock(EncaminhamentoService.class);
        HospitalController controller = new HospitalController(service);
        List<EncaminhamentoResponse.HospitalDTO> hospitais = List.of(
                EncaminhamentoResponse.HospitalDTO.builder().coCnes("1").build()
        );
        when(service.buscarHospitais("3131307", "74", 1))
                .thenReturn(EncaminhamentoResponse.builder().hospitais(hospitais).build());

        assertThat(controller.getHospitais("3131307", "74", 1)).isEqualTo(hospitais);
    }

    @Test
    void getHospitaisCapitais_retornaListaDoService() {
        EncaminhamentoService service = mock(EncaminhamentoService.class);
        HospitalController controller = new HospitalController(service);
        List<EncaminhamentoResponse.HospitalDTO> hospitais = List.of(
                EncaminhamentoResponse.HospitalDTO.builder().coCnes("2").build()
        );
        when(service.buscarHospitaisDasCapitais("MG")).thenReturn(hospitais);

        assertThat(controller.getHospitaisCapitais("MG")).isEqualTo(hospitais);
    }
}
