package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.RankingResponse;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.service.EstadoHistoricoService;
import br.com.fiap.vigisus.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ranking")
@Tag(name = "Ranking Municipal")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;
    private final EstadoHistoricoService estadoHistoricoService;

    @GetMapping
    @Operation(
            summary = "Ranking municipal por incidência",
            description = """
                    Retorna municípios ordenados por incidência de dengue por
                    100 mil habitantes. Útil para identificar regiões de maior
                    pressão epidemiológica.

                    Fontes: SINAN (casos), IBGE (população estimada).
                    Retorna contexto informacional baseado em dados públicos do SUS.
                    Não realiza diagnóstico, triagem clínica nem define conduta médica.
                    A decisão final permanece com o profissional de saúde habilitado.
                    """)
    public RankingResponse getRanking(
            @RequestParam String uf,
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano,
            @RequestParam(defaultValue = "20") int top,
            @RequestParam(defaultValue = "piores") String ordem) {

        if (ano == null) {
            ano = LocalDate.now().getYear();
        }

        return rankingService.calcularRanking(uf, doenca, ano, top, ordem);
    }

    @GetMapping("/estado-historico")
    @Operation(
            summary = "Histórico agregado do estado por semana",
            description = "Retorna série semanal, total anual, incidência e classificação agregadas para o estado informado."
    )
    public PerfilEpidemiologicoResponse getHistoricoEstado(
            @RequestParam String uf,
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {

        if (ano == null) {
            ano = LocalDate.now().getYear();
        }

        return estadoHistoricoService.gerarPerfilEstado(uf, doenca, ano);
    }
}
