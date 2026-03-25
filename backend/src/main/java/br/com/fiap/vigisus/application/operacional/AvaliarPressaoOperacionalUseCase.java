package br.com.fiap.vigisus.application.operacional;

import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import br.com.fiap.vigisus.service.PressaoOperacionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvaliarPressaoOperacionalUseCase {

    private final PressaoOperacionalService pressaoOperacionalService;

    public PressaoOperacionalResponse executar(PressaoOperacionalRequest request) {
        return pressaoOperacionalService.avaliarPressao(request);
    }
}
