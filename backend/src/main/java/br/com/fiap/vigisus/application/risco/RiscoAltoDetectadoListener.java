package br.com.fiap.vigisus.application.risco;

import br.com.fiap.vigisus.domain.risco.RiscoAltoDetectadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener que reage ao evento de risco alto detectado.
 *
 * Versão atual: apenas registra log estruturado para monitoramento.
 * Versão 2.0: envio de alerta para gestores municipais cadastrados.
 */
@Slf4j
@Component
public class RiscoAltoDetectadoListener {

    @EventListener
    public void onRiscoAltoDetectado(RiscoAltoDetectadoEvent event) {
        log.warn(
            "ALERTA EPIDEMIOLÓGICO | Município: {} ({}/{}) | " +
            "Classificação: {} | Score: {} | " +
            "Temp: {}°C | Chuva: {}mm | Data: {}",
            event.nomeMunicipio(), event.coIbge(), event.uf(),
            event.classificacao(), event.scoreRisco(),
            event.temperaturaMedia(), event.chuvaAcumulada(),
            event.dataDeteccao()
        );
    }
}
