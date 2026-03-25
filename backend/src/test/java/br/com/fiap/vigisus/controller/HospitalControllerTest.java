package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.encaminhamento.ConsultarHospitaisCapitaisUseCase;
import br.com.fiap.vigisus.application.encaminhamento.ConsultarHospitaisUseCase;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HospitalControllerTest {

    @Test
    void getHospitais_delegaAoUseCase() {
        ConsultarHospitaisUseCase consultarHospitaisUseCase = mock(ConsultarHospitaisUseCase.class);
        ConsultarHospitaisCapitaisUseCase consultarHospitaisCapitaisUseCase = mock(ConsultarHospitaisCapitaisUseCase.class);
        HospitalController controller = new HospitalController(consultarHospitaisUseCase, consultarHospitaisCapitaisUseCase);
        List<EncaminhamentoResponse.HospitalDTO> hospitais = List.of(
                EncaminhamentoResponse.HospitalDTO.builder().coCnes("1").build()
        );
        when(consultarHospitaisUseCase.executar("3131307", "74", 1)).thenReturn(hospitais);

        assertThat(controller.getHospitais("3131307", "74", 1)).isEqualTo(hospitais);
    }

    @Test
    void getHospitaisCapitais_delegaAoUseCase() {
        ConsultarHospitaisUseCase consultarHospitaisUseCase = mock(ConsultarHospitaisUseCase.class);
        ConsultarHospitaisCapitaisUseCase consultarHospitaisCapitaisUseCase = mock(ConsultarHospitaisCapitaisUseCase.class);
        HospitalController controller = new HospitalController(consultarHospitaisUseCase, consultarHospitaisCapitaisUseCase);
        List<EncaminhamentoResponse.HospitalDTO> hospitais = List.of(
                EncaminhamentoResponse.HospitalDTO.builder().coCnes("2").build()
        );
        when(consultarHospitaisCapitaisUseCase.executar("MG")).thenReturn(hospitais);

        assertThat(controller.getHospitaisCapitais("MG")).isEqualTo(hospitais);
    }
}
