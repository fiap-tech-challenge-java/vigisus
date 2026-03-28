package br.com.fiap.vigisus.application.epidemiologia;

import br.com.fiap.vigisus.dto.RankingResponse;
import br.com.fiap.vigisus.service.RankingService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ConsultarRankingMunicipalUseCase {

    private final RankingService rankingService;
    private final MeterRegistry meterRegistry;

    public RankingResponse buscar(String uf, String doenca, Integer ano, int top, String ordem) {
        RankingResponse response = rankingService.calcularRanking(uf, doenca, resolverAno(ano), top, ordem);

        meterRegistry.counter("vigisus.buscas.ranking",
                "uf", uf.toUpperCase()
        ).increment();

        return response;
    }

    private int resolverAno(Integer ano) {
        return ano != null ? ano : LocalDate.now().getYear();
    }
}
