package br.com.fiap.vigisus.application.triagem;

import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
import br.com.fiap.vigisus.service.TriagemService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvaliarTriagemUseCase {

    private final TriagemService triagemService;
    private final MeterRegistry meterRegistry;

    public TriagemResponse executar(TriagemRequest request) {
        TriagemResponse response = triagemService.avaliar(request);

        meterRegistry.counter("vigisus.triagens.avaliadas",
                "prioridade", response.getPrioridade() != null ? response.getPrioridade() : "DESCONHECIDA"
        ).increment();

        return response;
    }
}
