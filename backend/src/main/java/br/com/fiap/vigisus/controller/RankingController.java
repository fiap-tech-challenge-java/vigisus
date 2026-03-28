package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarHistoricoEstadoUseCase;
import br.com.fiap.vigisus.application.epidemiologia.ConsultarRankingMunicipalUseCase;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.RankingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranking")
@Tag(name = "Epidemiologia", description = "Histórico e ranking epidemiológico por município, estado e Brasil")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RankingController {

    private final ConsultarRankingMunicipalUseCase consultarRankingMunicipalUseCase;
    private final ConsultarHistoricoEstadoUseCase consultarHistoricoEstadoUseCase;

    @GetMapping
    @Operation(
            summary = "Ranking municipal por incidencia",
            description = """
                    Retorna municipios ordenados por incidencia de dengue por
                    100 mil habitantes. Util para identificar regioes de maior
                    pressao epidemiologica.

                    Fontes: SINAN (casos), IBGE (populacao estimada).
                    Retorna contexto informacional baseado em dados publicos do SUS.
                    Nao realiza diagnostico, triagem clinica nem define conduta medica.
                    A decisao final permanece com o profissional de saude habilitado.
                    """)
    public RankingResponse getRanking(
            @RequestParam String uf,
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano,
            @RequestParam(defaultValue = "20") int top,
            @RequestParam(defaultValue = "piores") String ordem) {
        return consultarRankingMunicipalUseCase.buscar(uf, doenca, ano, top, ordem);
    }

    @GetMapping("/estado-historico")
    @Operation(
            summary = "Historico agregado do estado por semana",
            description = "Retorna serie semanal, total anual, incidencia e classificacao agregadas para o estado informado."
    )
    public PerfilEpidemiologicoResponse getHistoricoEstado(
            @RequestParam String uf,
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {
        return consultarHistoricoEstadoUseCase.buscar(uf, doenca, ano);
    }
}
