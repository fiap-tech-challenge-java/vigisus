package br.com.fiap.vigisus.application.epidemiologia;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.PerfilEpidemiologicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ConsultarPerfilEpidemiologicoUseCase {

    private final PerfilEpidemiologicoService perfilService;
    private final IaService iaService;

    public PerfilEpidemiologicoResponse buscarMunicipio(String coIbge, String doenca, Integer ano) {
        PerfilEpidemiologicoResponse perfil = perfilService.gerarPerfil(coIbge, doenca, resolverAno(ano));
        perfil.setTextoIa(iaService.gerarTextoEpidemiologico(perfil));
        return perfil;
    }

    private int resolverAno(Integer ano) {
        return ano != null ? ano : LocalDate.now().getYear();
    }
}
