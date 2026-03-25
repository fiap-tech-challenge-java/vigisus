package br.com.fiap.vigisus.application.encaminhamento;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarHospitaisUseCaseTest {

    @Test
    void executar_retornaListaDoService() {
        EncaminhamentoService service = mock(EncaminhamentoService.class);
        ConsultarHospitaisUseCase useCase = new ConsultarHospitaisUseCase(service);
        List<EncaminhamentoResponse.HospitalDTO> hospitais = List.of(
                EncaminhamentoResponse.HospitalDTO.builder().coCnes("1").build()
        );
        when(service.buscarHospitais("3131307", "74", 1))
                .thenReturn(EncaminhamentoResponse.builder().hospitais(hospitais).build());

        assertThat(useCase.executar("3131307", "74", 1)).isEqualTo(hospitais);
    }
}
