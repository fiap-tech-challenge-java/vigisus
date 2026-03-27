package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarPerfilEpidemiologicoUseCase;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
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
@RequestMapping("/api/perfil")
@Tag(name = "Epidemiologia", description = "Histórico e ranking epidemiológico por município, estado e Brasil")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PerfilController {

    private final ConsultarPerfilEpidemiologicoUseCase consultarPerfilEpidemiologicoUseCase;

    @GetMapping("/{coIbge}")
    @Operation(
            summary = "Perfil epidemiologico do municipio",
            description = """
                    Retorna o historico de casos de dengue por semana epidemiologica,
                    incidencia por 100 mil habitantes, classificacao e posicao no
                    ranking estadual.

                    Fontes: SINAN (casos notificados), IBGE (populacao).
                    Retorna contexto informacional baseado em dados publicos do SUS.
                    Nao realiza diagnostico, triagem clinica nem define conduta medica.
                    A decisao final permanece com o profissional de saude habilitado.
                    """)
    public PerfilEpidemiologicoResponse getPerfil(
            @PathVariable String coIbge,
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {
        return consultarPerfilEpidemiologicoUseCase.buscarMunicipio(coIbge, doenca, ano);
    }
}
