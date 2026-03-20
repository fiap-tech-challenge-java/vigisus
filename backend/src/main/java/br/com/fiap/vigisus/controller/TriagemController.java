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
            summary = "Contexto clínico-epidemiológico",
            description = """
                    Organiza sintomas informados com o contexto epidemiológico
                    real do município para apoio informacional ao profissional.

                    NÃO realiza triagem clínica, diagnóstico ou classificação
                    de risco clínico. A avaliação e decisão são exclusivamente
                    do profissional de saúde habilitado.

                    Retorna contexto informacional baseado em dados públicos do SUS.
                    Fontes: SINAN (contexto epidemiológico), CNES (hospitais).
                    """)
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
