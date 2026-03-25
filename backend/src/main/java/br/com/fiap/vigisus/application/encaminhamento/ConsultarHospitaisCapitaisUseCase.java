package br.com.fiap.vigisus.application.encaminhamento;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConsultarHospitaisCapitaisUseCase {

    private final EncaminhamentoService encaminhamentoService;

    public List<EncaminhamentoResponse.HospitalDTO> executar(String uf) {
        return encaminhamentoService.buscarHospitaisDasCapitais(uf);
    }
}
