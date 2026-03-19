package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/previsao-risco")
@Tag(name = "Previsão de Risco")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PrevisaoRiscoController {

    private final PrevisaoRiscoService previsaoRiscoService;
    private final IaService iaService;

    @GetMapping("/{coIbge}")
    @Operation(summary = "Calcula o risco epidemiológico para as próximas 2 semanas cruzando histórico do SINAN com previsão climática")
    public PrevisaoRiscoResponse getPrevisaoRisco(
            @PathVariable String coIbge,
            @RequestParam(defaultValue = "dengue") String doenca) {

        PrevisaoRiscoResponse previsao = previsaoRiscoService.calcularRisco(coIbge);
        previsao.setTextoIa(iaService.gerarTextoRisco(previsao));
        return previsao;
    }
}
