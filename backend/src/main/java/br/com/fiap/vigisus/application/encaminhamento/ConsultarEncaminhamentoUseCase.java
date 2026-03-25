package br.com.fiap.vigisus.application.encaminhamento;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsultarEncaminhamentoUseCase {

    private final EncaminhamentoService encaminhamentoService;

    public EncaminhamentoResponse executar(String municipio, String condicao, String gravidade, String tpLeito, int minLeitosSus) {
        String leito = (tpLeito != null && !tpLeito.isBlank())
                ? tpLeito
                : encaminhamentoService.resolverTpLeito(gravidade);
        return encaminhamentoService.buscarHospitais(municipio, leito, minLeitosSus);
    }
}
