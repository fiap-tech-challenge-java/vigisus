package br.com.fiap.vigisus.application.risco;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsultarPrevisaoRiscoUseCase {

    private final PrevisaoRiscoService previsaoRiscoService;
    private final IaService iaService;

    public PrevisaoRiscoResponse buscarPorMunicipio(String coIbge) {
        PrevisaoRiscoResponse previsao = previsaoRiscoService.calcularRisco(coIbge);
        previsao.setTextoIa(iaService.gerarTextoRisco(previsao));
        return previsao;
    }
}
