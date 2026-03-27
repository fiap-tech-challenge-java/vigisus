package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.encaminhamento.ConsultarEncaminhamentoUseCase;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/encaminhar")
@Tag(name = "Encaminhamento", description = "Encaminhamento hospitalar data-driven com cálculo de distância")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EncaminhamentoController {

    private final ConsultarEncaminhamentoUseCase consultarEncaminhamentoUseCase;

    @GetMapping
    @Operation(
            summary = "Encaminhar paciente para hospital",
            description = "Localiza o hospital mais próximo com leito SUS disponível usando Fórmula de Haversine")
    public EncaminhamentoResponse getEncaminhamento(
            @RequestParam String municipio,
            @RequestParam(defaultValue = "dengue") String condicao,
            @RequestParam(defaultValue = "moderada") String gravidade,
            @RequestParam(required = false) String tpLeito,
            @RequestParam(defaultValue = "1") int minLeitosSus) {
        return consultarEncaminhamentoUseCase.executar(municipio, condicao, gravidade, tpLeito, minLeitosSus);
    }
}
