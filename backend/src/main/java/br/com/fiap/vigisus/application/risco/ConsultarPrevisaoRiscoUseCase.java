package br.com.fiap.vigisus.application.risco;

import br.com.fiap.vigisus.domain.risco.RiscoAltoDetectadoEvent;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ConsultarPrevisaoRiscoUseCase {

    private final PrevisaoRiscoService previsaoRiscoService;
    private final IaService iaService;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    public PrevisaoRiscoResponse buscarPorMunicipio(String coIbge) {
        PrevisaoRiscoResponse previsao = previsaoRiscoService.calcularRisco(coIbge);
        previsao.setTextoIa(iaService.gerarTextoRisco(previsao));

        if ("ALTO".equals(previsao.getClassificacao()) ||
            "MUITO_ALTO".equals(previsao.getClassificacao())) {

            eventPublisher.publishEvent(new RiscoAltoDetectadoEvent(
                previsao.getCoIbge(),
                previsao.getMunicipio(),
                previsao.getUf(),
                previsao.getClassificacao(),
                previsao.getScore(),
                previsao.getTemperaturaMedia() != null ? previsao.getTemperaturaMedia() : 0.0,
                previsao.getChuvaAcumulada() != null ? previsao.getChuvaAcumulada() : 0.0,
                LocalDate.now()
            ));
        }

        meterRegistry.counter("vigisus.buscas.risco",
                "coIbge", coIbge,
                "classificacao_risco", previsao.getClassificacao() != null ? previsao.getClassificacao() : "DESCONHECIDO"
        ).increment();

        return previsao;
    }
}
