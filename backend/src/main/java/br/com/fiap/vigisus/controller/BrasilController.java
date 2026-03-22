package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.service.BrasilEpidemiologicoService;
import br.com.fiap.vigisus.service.IaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/brasil")
@Tag(name = "Perfil Epidemiológico Brasil")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BrasilController {

    private final BrasilEpidemiologicoService brasilService;
    private final IaService iaService;

    @GetMapping("/casos")
    @Operation(
            summary = "Perfil epidemiológico do Brasil",
            description = """
                    Retorna agregação de casos de dengue de todo o Brasil,
                    com histórico por semana epidemiológica, principais estados
                    afetados e municípios em situação crítica.

                    Fontes: SINAN (casos notificados), IBGE (população).
                    Retorna contexto informacional baseado em dados públicos do SUS.
                    Não realiza diagnóstico, triagem clínica nem define conduta médica.
                    A decisão final permanece com o profissional de saúde habilitado.
                    """)
    public BrasilEpidemiologicoResponse getCasosBrasil(
            @RequestParam(defaultValue = "dengue") String doenca,
            @RequestParam(required = false) Integer ano) {

        if (ano == null) {
            ano = LocalDate.now().getYear();
        }

        BrasilEpidemiologicoResponse perfil = brasilService.gerarPerfilBrasil(doenca, ano);
        perfil.setTextoIa(iaService.gerarTextoEpidemiologico(
                br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse.builder()
                        .municipio("Brasil")
                        .uf("BR")
                        .doenca(doenca)
                        .ano(ano)
                        .total(perfil.getTotalCasos())
                        .incidencia(perfil.getIncidencia())
                        .classificacao(perfil.getClassificacao())
                        .tendencia(perfil.getTendencia())
                        .semanas(perfil.getSemanas())
                        .semanasAnoAnterior(perfil.getSemanasAnoAnterior())
                        .build()
        ));
        return perfil;
    }
}
