package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.triagem.AvaliarTriagemUseCase;
import br.com.fiap.vigisus.application.triagem.ConsultarCatalogoTriagemUseCase;
import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
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

    private final AvaliarTriagemUseCase avaliarTriagemUseCase;
    private final ConsultarCatalogoTriagemUseCase consultarCatalogoTriagemUseCase;

    @PostMapping("/avaliar")
    @Operation(
            summary = "Contexto clinico-epidemiologico",
            description = """
                    Organiza sintomas informados com o contexto epidemiologico
                    real do municipio para apoio informacional ao profissional.

                    NAO realiza triagem clinica, diagnostico ou classificacao
                    de risco clinico. A avaliacao e decisao sao exclusivamente
                    do profissional de saude habilitado.

                    Retorna contexto informacional baseado em dados publicos do SUS.
                    Fontes: SINAN (contexto epidemiologico), CNES (hospitais).
                    """)
    public TriagemResponse avaliar(@Valid @RequestBody TriagemRequest request) {
        return avaliarTriagemUseCase.executar(request);
    }

    @GetMapping("/sintomas-validos")
    @Operation(summary = "Lista sintomas aceitos pelo sistema")
    public Map<String, List<String>> sintomas() {
        return consultarCatalogoTriagemUseCase.executar();
    }
}
