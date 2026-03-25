package br.com.fiap.vigisus.application.encaminhamento;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConsultarHospitaisUseCase {

    private final EncaminhamentoService encaminhamentoService;

    public List<EncaminhamentoResponse.HospitalDTO> executar(String municipio, String tpLeito, int minLeitosSus) {
        return encaminhamentoService.buscarHospitais(municipio, tpLeito, minLeitosSus).getHospitais();
    }
}
