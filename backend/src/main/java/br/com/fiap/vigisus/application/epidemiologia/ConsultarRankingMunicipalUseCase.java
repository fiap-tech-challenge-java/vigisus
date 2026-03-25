package br.com.fiap.vigisus.application.epidemiologia;

import br.com.fiap.vigisus.dto.RankingResponse;
import br.com.fiap.vigisus.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ConsultarRankingMunicipalUseCase {

    private final RankingService rankingService;

    public RankingResponse buscar(String uf, String doenca, Integer ano, int top, String ordem) {
        return rankingService.calcularRanking(uf, doenca, resolverAno(ano), top, ordem);
    }

    private int resolverAno(Integer ano) {
        return ano != null ? ano : LocalDate.now().getYear();
    }
}
