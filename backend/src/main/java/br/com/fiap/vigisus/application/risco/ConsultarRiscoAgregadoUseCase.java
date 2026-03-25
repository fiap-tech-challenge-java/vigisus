package br.com.fiap.vigisus.application.risco;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.RiscoAgregadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarRiscoAgregadoUseCase {

    private final RiscoAgregadoService riscoAgregadoService;
    private final IaService iaService;

    public PrevisaoRiscoResponse buscarBrasil() {
        PrevisaoRiscoResponse risco = riscoAgregadoService.calcularRiscoBrasil();
        if (risco != null) {
            risco.setTextoIa(iaService.gerarTextoRisco(risco));
        }
        return risco;
    }

    public PrevisaoRiscoResponse buscarEstado(String uf) {
        PrevisaoRiscoResponse risco = riscoAgregadoService.calcularRiscoEstado(uf);
        if (risco != null) {
            risco.setTextoIa(iaService.gerarTextoRisco(risco));
        }
        return risco;
    }

    public List<Estabelecimento> buscarHospitaisBrasil() {
        return riscoAgregadoService.buscarHospitaisBrasil();
    }

    public List<Estabelecimento> buscarHospitaisEstado(String uf) {
        return riscoAgregadoService.buscarHospitaisEstado(uf);
    }
}
