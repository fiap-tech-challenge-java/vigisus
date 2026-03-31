package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarBrasilEpidemiologicoUseCase;
import br.com.fiap.vigisus.application.epidemiologia.ConsultarRankingMunicipalUseCase;
import br.com.fiap.vigisus.dto.AdminBuscaIaDTO;
import br.com.fiap.vigisus.dto.AdminResumoDTO;
import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.EstadoDTO;
import br.com.fiap.vigisus.dto.MunicipioRiscoDTO;
import br.com.fiap.vigisus.dto.RankingMunicipioDTO;
import br.com.fiap.vigisus.dto.RankingResponse;
import br.com.fiap.vigisus.service.IaBuscaTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Dashboard", description = "Endpoints do painel administrativo de monitoramento epidemiológico")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final ConsultarBrasilEpidemiologicoUseCase consultarBrasilUseCase;
    private final ConsultarRankingMunicipalUseCase consultarRankingUseCase;
    private final IaBuscaTracker iaBuscaTracker;

    @GetMapping("/resumo")
    @Operation(
            summary = "KPIs nacionais",
            description = "Retorna os indicadores-chave nacionais: total de casos, incidência, classificação, tendência, municípios e estados afetados")
    public AdminResumoDTO getResumo(
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {
        int anoConsulta = ano != null ? ano : LocalDate.now().getYear();
        BrasilEpidemiologicoResponse brasil = consultarBrasilUseCase.buscar(doenca, anoConsulta);

        long municipiosAltoRisco = 0;
        long municipiosEpidemia = 0;
        int totalEstados = 0;

        if (brasil.getEstadosPiores() != null) {
            totalEstados = brasil.getEstadosPiores().size();
        }
        if (brasil.getMunicipiosPiores() != null) {
            municipiosAltoRisco = brasil.getMunicipiosPiores().stream()
                    .filter(m -> "ALTO".equalsIgnoreCase(m.getClassificacao())
                            || "MUITO_ALTO".equalsIgnoreCase(m.getClassificacao()))
                    .count();
            municipiosEpidemia = brasil.getMunicipiosPiores().stream()
                    .filter(m -> "EPIDEMIA".equalsIgnoreCase(m.getClassificacao()))
                    .count();
        }

        return AdminResumoDTO.builder()
                .totalCasos(brasil.getTotalCasos())
                .incidenciaNacional(brasil.getIncidencia())
                .classificacaoNacional(brasil.getClassificacao())
                .tendencia(brasil.getTendencia())
                .totalMunicipiosComDados(0)
                .totalEstadosAfetados(totalEstados)
                .municipiosAltoRisco((int) municipiosAltoRisco)
                .municipiosEpidemia((int) municipiosEpidemia)
                .doenca(brasil.getDoenca())
                .ano(brasil.getAno())
                .build();
    }

    @GetMapping("/top-municipios")
    @Operation(
            summary = "Top N municípios por incidência",
            description = "Retorna os municípios com maior incidência de dengue para uso no gráfico de ranking")
    public List<RankingMunicipioDTO> getTopMunicipios(
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {
        RankingResponse ranking = consultarRankingUseCase.buscar(null, doenca, ano, top, "piores");
        return ranking.getRanking() != null ? ranking.getRanking() : List.of();
    }

    @GetMapping("/top-estados")
    @Operation(
            summary = "Top estados por total de casos",
            description = "Retorna os estados com maior número de casos para uso no mapa e gráfico de barras")
    public List<EstadoDTO> getTopEstados(
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {
        int anoConsulta = ano != null ? ano : LocalDate.now().getYear();
        BrasilEpidemiologicoResponse brasil = consultarBrasilUseCase.buscar(doenca, anoConsulta);
        return brasil.getEstadosPiores() != null ? brasil.getEstadosPiores() : List.of();
    }

    @GetMapping("/municipios-risco")
    @Operation(
            summary = "Municípios classificados por risco",
            description = "Retorna a lista de municípios críticos com classificação de risco (EPIDEMIA / ALTO / MODERADO / BAIXO)")
    public List<MunicipioRiscoDTO> getMunicipiosRisco(
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {
        int anoConsulta = ano != null ? ano : LocalDate.now().getYear();
        BrasilEpidemiologicoResponse brasil = consultarBrasilUseCase.buscar(doenca, anoConsulta);
        return brasil.getMunicipiosPiores() != null ? brasil.getMunicipiosPiores() : List.of();
    }

    @GetMapping("/buscas-ia")
    @Operation(
            summary = "Perguntas mais frequentes feitas à IA",
            description = "Retorna o ranking das perguntas mais frequentes submetidas ao endpoint /api/busca desde a última inicialização do servidor")
    public List<AdminBuscaIaDTO> getBuscasIa(
            @RequestParam(defaultValue = "20") int top) {
        return iaBuscaTracker.listarMaisFrequentes(top);
    }
}
