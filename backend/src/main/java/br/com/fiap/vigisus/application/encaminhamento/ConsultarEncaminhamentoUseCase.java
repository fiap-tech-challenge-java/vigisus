package br.com.fiap.vigisus.application.encaminhamento;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsultarEncaminhamentoUseCase {

    private final EncaminhamentoService encaminhamentoService;
    private final MeterRegistry meterRegistry;

    public EncaminhamentoResponse executar(String municipio, String condicao, String gravidade, String tpLeito, int minLeitosSus) {
        String leito = (tpLeito != null && !tpLeito.isBlank())
                ? tpLeito
                : encaminhamentoService.resolverTpLeito(gravidade);

        meterRegistry.counter("vigisus.encaminhamentos",
                "tipo_leito", leito
        ).increment();

        return encaminhamentoService.buscarHospitais(municipio, leito, minLeitosSus);
    }
}
