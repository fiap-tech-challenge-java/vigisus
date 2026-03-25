package br.com.fiap.vigisus.application.dashboard;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarBrasilEpidemiologicoUseCase;
import br.com.fiap.vigisus.application.risco.ConsultarRiscoAgregadoUseCase;
import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.DashboardBrasilResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Estabelecimento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ConsultarDashboardBrasilUseCase {

    private final ConsultarBrasilEpidemiologicoUseCase consultarBrasilEpidemiologicoUseCase;
    private final ConsultarRiscoAgregadoUseCase consultarRiscoAgregadoUseCase;

    public DashboardBrasilResponse buscar(String doenca, Integer ano) {
        CompletableFuture<BrasilEpidemiologicoResponse> brasilFuture = CompletableFuture.supplyAsync(
                () -> consultarBrasilEpidemiologicoUseCase.buscar(doenca, ano)
        );
        CompletableFuture<PrevisaoRiscoResponse> riscoFuture = CompletableFuture.supplyAsync(
                consultarRiscoAgregadoUseCase::buscarBrasil
        );
        CompletableFuture<List<Estabelecimento>> hospitaisFuture = CompletableFuture.supplyAsync(
                consultarRiscoAgregadoUseCase::buscarHospitaisBrasil
        );

        CompletableFuture.allOf(brasilFuture, riscoFuture, hospitaisFuture).join();

        return DashboardBrasilResponse.builder()
                .brasil(brasilFuture.join())
                .risco(riscoFuture.join())
                .hospitais(hospitaisFuture.join())
                .build();
    }
}
