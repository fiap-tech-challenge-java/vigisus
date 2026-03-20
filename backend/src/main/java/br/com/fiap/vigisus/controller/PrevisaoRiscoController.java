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
    @Operation(
            summary = "Previsão de risco epidemiológico",
            description = """
                    Calcula score de risco para as próximas 2 semanas cruzando
                    padrão sazonal histórico (SINAN) com previsão climática
                    (Open-Meteo). Baseado em evidência científica publicada sobre
                    condições favoráveis ao Aedes aegypti.

                    Score 0-8: 0-1 Baixo · 2-3 Moderado · 4-5 Alto · 6+ Muito Alto.
                    Retorna contexto informacional baseado em dados públicos do SUS.
                    Não realiza diagnóstico, triagem clínica nem define conduta médica.
                    A decisão final permanece com o profissional de saúde habilitado.
                    """)
    public PrevisaoRiscoResponse getPrevisaoRisco(
            @PathVariable String coIbge,
            @RequestParam(defaultValue = "dengue") String doenca) {

        PrevisaoRiscoResponse previsao = previsaoRiscoService.calcularRisco(coIbge);
        previsao.setTextoIa(iaService.gerarTextoRisco(previsao));
        return previsao;
    }
}
