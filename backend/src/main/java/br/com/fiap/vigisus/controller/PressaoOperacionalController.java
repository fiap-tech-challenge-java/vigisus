package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import br.com.fiap.vigisus.service.PressaoOperacionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operacional")
@Tag(name = "Painel Operacional")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PressaoOperacionalController {

    private final PressaoOperacionalService pressaoOperacionalService;

    @PostMapping("/pressao")
    @Operation(
            summary = "Contexto operacional da unidade de saúde",
            description = """
                    Organiza dados epidemiológicos, climáticos e de infraestrutura
                    em um contexto informacional consolidado para apoio à decisão
                    do profissional de saúde.

                    NÃO realiza triagem clínica, diagnóstico ou definição de conduta.
                    A decisão final permanece com o profissional de saúde habilitado.

                    Fontes: SINAN (casos), CNES (hospitais), Open-Meteo (clima), IBGE (população).
                    """)
    public PressaoOperacionalResponse avaliarPressao(@RequestBody PressaoOperacionalRequest request) {
        return pressaoOperacionalService.avaliarPressao(request);
    }

    @GetMapping("/protocolo-surto")
    @Operation(summary = "Retorna checklist do protocolo de surto de dengue")
    public Map<String, Object> getProtocoloSurto() {
        return Map.of(
                "titulo", "Protocolo de Surto Dengue — MS 2024",
                "passos", List.of(
                        "1. Notificar Vigilância Epidemiológica Municipal",
                        "2. Registrar casos suspeitos no SINAN",
                        "3. Acionar Central de Regulação para leitos de referência",
                        "4. Solicitar nebulização à Secretaria de Saúde",
                        "5. Ativar busca ativa nas áreas de maior incidência",
                        "6. Abrir sala de situação com reunião diária"),
                "contatos", Map.of(
                        "vigilancia_epidemiologica", "0800-644-6645",
                        "central_regulacao_mg", "(31) 3916-6868"));
    }
}
