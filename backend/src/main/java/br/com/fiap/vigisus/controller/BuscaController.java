package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.busca.BuscaCompletaUseCase;
import br.com.fiap.vigisus.dto.BuscaCompletaResponse;
import br.com.fiap.vigisus.dto.BuscaRequest;
import br.com.fiap.vigisus.service.IaBuscaTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/busca")
@Tag(name = "Busca por Linguagem Natural", description = "Interface conversacional com IA — pergunte em português livre")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BuscaController {

    private final BuscaCompletaUseCase buscaCompletaUseCase;
    private final IaBuscaTracker iaBuscaTracker;

    @PostMapping
    @Operation(
            summary = "Busca por linguagem natural",
            description = "Interpreta a pergunta, consulta a base de dados e retorna resposta narrativa gerada por IA")
    public BuscaCompletaResponse buscar(@Valid @RequestBody BuscaRequest request) {
        iaBuscaTracker.registrar(request.getPergunta());
        return buscaCompletaUseCase.buscarPorPergunta(request.getPergunta());
    }

    @GetMapping("/perfil-direto")
    @Operation(summary = "Busca perfil por municipio sem depender de IA")
    public ResponseEntity<BuscaCompletaResponse> buscarDireto(
            @RequestParam String municipio,
            @RequestParam String uf,
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {
        return ResponseEntity.ok(buscaCompletaUseCase.buscarDireto(municipio, uf, doenca, ano));
    }
}
