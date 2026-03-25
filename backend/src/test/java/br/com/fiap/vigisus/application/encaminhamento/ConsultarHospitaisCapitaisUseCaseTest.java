package br.com.fiap.vigisus.application.encaminhamento;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarHospitaisCapitaisUseCaseTest {

    @Test
    void executar_retornaListaDoService() {
        EncaminhamentoService service = mock(EncaminhamentoService.class);
        ConsultarHospitaisCapitaisUseCase useCase = new ConsultarHospitaisCapitaisUseCase(service);
        List<EncaminhamentoResponse.HospitalDTO> hospitais = List.of(
                EncaminhamentoResponse.HospitalDTO.builder().coCnes("2").build()
        );
        when(service.buscarHospitaisDasCapitais("MG")).thenReturn(hospitais);

        assertThat(useCase.executar("MG")).isEqualTo(hospitais);
    }
}
