package br.com.fiap.vigisus.application.risco;

import br.com.fiap.vigisus.domain.risco.RiscoAltoDetectadoEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RiscoAltoDetectadoListenerTest {

    private final RiscoAltoDetectadoListener listener =
        new RiscoAltoDetectadoListener();

    @Test
    void deveProcessarEventoSemLancarExcecao() {
        var event = new RiscoAltoDetectadoEvent(
            "3550308", "Campinas", "SP",
            "ALTO", 75.0, 32.5, 280.0,
            LocalDate.now()
        );
        assertDoesNotThrow(() -> listener.onRiscoAltoDetectado(event));
    }

    @Test
    void deveIdentificarEventoMuitoAlto() {
        var event = new RiscoAltoDetectadoEvent(
            "3550308", "Campinas", "SP",
            "MUITO_ALTO", 92.0, 35.0, 420.0,
            LocalDate.now()
        );
        assertThat(event.isMuitoAlto()).isTrue();
    }
}
