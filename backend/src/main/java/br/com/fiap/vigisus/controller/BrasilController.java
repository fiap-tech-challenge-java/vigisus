package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarBrasilEpidemiologicoUseCase;
import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brasil")
@Tag(name = "Perfil Epidemiologico Brasil")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BrasilController {

    private final ConsultarBrasilEpidemiologicoUseCase consultarBrasilEpidemiologicoUseCase;

    @GetMapping("/casos")
    @Operation(
            summary = "Perfil epidemiologico do Brasil",
            description = """
                    Retorna agregacao de casos de dengue de todo o Brasil,
                    com historico por semana epidemiologica, principais estados
                    afetados e municipios em situacao critica.

                    Fontes: SINAN (casos notificados), IBGE (populacao).
                    Retorna contexto informacional baseado em dados publicos do SUS.
                    Nao realiza diagnostico, triagem clinica nem define conduta medica.
                    A decisao final permanece com o profissional de saude habilitado.
                    """)
    public BrasilEpidemiologicoResponse getCasosBrasil(
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {
        return consultarBrasilEpidemiologicoUseCase.buscar(doenca, ano);
    }
}
