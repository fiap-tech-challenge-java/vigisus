package br.com.fiap.vigisus.application.epidemiologia;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.service.EstadoHistoricoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ConsultarHistoricoEstadoUseCase {

    private final EstadoHistoricoService estadoHistoricoService;

    public PerfilEpidemiologicoResponse buscar(String uf, String doenca, Integer ano) {
        return estadoHistoricoService.gerarPerfilEstado(uf, doenca, resolverAno(ano));
    }

    private int resolverAno(Integer ano) {
        return ano != null ? ano : LocalDate.now().getYear();
    }
}
