package br.com.fiap.vigisus.application.epidemiologia;

import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.service.BrasilEpidemiologicoService;
import br.com.fiap.vigisus.service.IaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ConsultarBrasilEpidemiologicoUseCase {

    private final BrasilEpidemiologicoService brasilService;
    private final IaService iaService;

    public BrasilEpidemiologicoResponse buscar(String doenca, Integer ano) {
        BrasilEpidemiologicoResponse perfil = brasilService.gerarPerfilBrasil(doenca, resolverAno(ano));
        perfil.setTextoIa(iaService.gerarTextoEpidemiologico(montarPerfilParaNarrativa(perfil)));
        return perfil;
    }

    private PerfilEpidemiologicoResponse montarPerfilParaNarrativa(BrasilEpidemiologicoResponse perfil) {
        return PerfilEpidemiologicoResponse.builder()
                .municipio("Brasil")
                .uf("BR")
                .doenca(perfil.getDoenca())
                .ano(perfil.getAno())
                .total(perfil.getTotalCasos())
                .incidencia(perfil.getIncidencia())
                .classificacao(perfil.getClassificacao())
                .tendencia(perfil.getTendencia())
                .semanas(perfil.getSemanas())
                .semanasAnoAnterior(perfil.getSemanasAnoAnterior())
                .build();
    }

    private int resolverAno(Integer ano) {
        return ano != null ? ano : LocalDate.now().getYear();
    }
}
