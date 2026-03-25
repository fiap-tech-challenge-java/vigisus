package br.com.fiap.vigisus.application.triagem;

import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
import br.com.fiap.vigisus.service.TriagemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvaliarTriagemUseCase {

    private final TriagemService triagemService;

    public TriagemResponse executar(TriagemRequest request) {
        return triagemService.avaliar(request);
    }
}
