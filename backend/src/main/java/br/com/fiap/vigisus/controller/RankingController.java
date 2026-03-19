package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.RankingResponse;
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

    @GetMapping
    @Operation(summary = "Retorna ranking de municípios por incidência de dengue por 100 mil habitantes")
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
}
