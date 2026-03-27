package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.operacional.AvaliarPressaoOperacionalUseCase;
import br.com.fiap.vigisus.application.operacional.ConsultarProtocoloSurtoUseCase;
import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/operacional")
@Tag(name = "Operacional", description = "Pressão assistencial e protocolo de surto")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PressaoOperacionalController {

    private final AvaliarPressaoOperacionalUseCase avaliarPressaoOperacionalUseCase;
    private final ConsultarProtocoloSurtoUseCase consultarProtocoloSurtoUseCase;

    @PostMapping("/pressao")
    @Operation(
            summary = "Contexto operacional da unidade de saude",
            description = """
                    Organiza dados epidemiologicos, climaticos e de infraestrutura
                    em um contexto informacional consolidado para apoio a decisao
                    do profissional de saude.

                    NAO realiza triagem clinica, diagnostico ou definicao de conduta.
                    A decisao final permanece com o profissional de saude habilitado.

                    Fontes: SINAN (casos), CNES (hospitais), Open-Meteo (clima), IBGE (populacao).
                    """)
    public PressaoOperacionalResponse avaliarPressao(@RequestBody PressaoOperacionalRequest request) {
        return avaliarPressaoOperacionalUseCase.executar(request);
    }

    @GetMapping("/protocolo-surto")
    @Operation(summary = "Retorna checklist do protocolo de surto de dengue")
    public Map<String, Object> getProtocoloSurto() {
        return consultarProtocoloSurtoUseCase.executar();
    }
}
