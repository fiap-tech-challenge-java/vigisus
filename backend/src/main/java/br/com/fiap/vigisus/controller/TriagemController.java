package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
import br.com.fiap.vigisus.service.TriagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/triagem")
@Tag(name = "Triagem Inteligente")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TriagemController {

    private final TriagemService triagemService;

    @PostMapping("/avaliar")
    @Operation(
            summary = "Avalia risco do paciente com base em sintomas + contexto epidemiológico real do município",
            description = "Não realiza diagnóstico. Contextualiza o risco clínico com dados reais do SINAN " +
                          "para apoiar a decisão do profissional de saúde."
    )
    public TriagemResponse avaliar(@Valid @RequestBody TriagemRequest request) {
        return triagemService.avaliar(request);
    }

    @GetMapping("/sintomas-validos")
    @Operation(summary = "Lista sintomas aceitos pelo sistema")
    public Map<String, List<String>> sintomas() {
        return Map.of(
                "sintomas", List.of(
                        "febre", "dor_muscular", "dor_retro_orbital", "cefaleia",
                        "exantema", "nausea", "vomito", "dor_abdominal",
                        "sangramento", "prostacao", "tontura", "falta_ar"
                ),
                "comorbidades", List.of(
                        "diabetes", "hipertensao", "obesidade", "gestante",
                        "doenca_renal", "doenca_cardiaca", "imunossupressao"
                )
        );
    }
}
