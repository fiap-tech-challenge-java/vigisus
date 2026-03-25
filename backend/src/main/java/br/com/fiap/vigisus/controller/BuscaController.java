package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.busca.BuscaCompletaUseCase;
import br.com.fiap.vigisus.dto.BuscaCompletaResponse;
import br.com.fiap.vigisus.dto.BuscaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Busca por Linguagem Natural")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BuscaController {

    private final BuscaCompletaUseCase buscaCompletaUseCase;

    @PostMapping
    @Operation(
            summary = "Busca completa por linguagem natural",
            description = """
                    Interpreta uma pergunta em portugues e retorna contexto
                    epidemiologico completo: historico de casos, risco climatico,
                    hospitais com estrutura disponivel e resumo narrativo gerado
                    por IA.

                    Exemplo de perguntas:
                      "dengue em Lavras MG 2024"
                      "situacao da dengue em Belo Horizonte"
                      "casos de dengue em Varginha nos ultimos anos"

                    Retorna contexto informacional baseado em dados publicos do SUS.
                    A IA narra dados existentes. Nao realiza diagnostico.
                    A decisao final permanece com o profissional de saude habilitado.
                    """)
    public BuscaCompletaResponse buscar(@RequestBody BuscaRequest request) {
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
