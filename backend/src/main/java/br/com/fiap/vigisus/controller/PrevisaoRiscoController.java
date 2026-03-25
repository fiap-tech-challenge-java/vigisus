package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.risco.ConsultarPrevisaoRiscoUseCase;
import br.com.fiap.vigisus.application.risco.ConsultarRiscoAgregadoUseCase;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Estabelecimento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/previsao-risco")
@Tag(name = "Previsao de Risco")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PrevisaoRiscoController {

    private final ConsultarPrevisaoRiscoUseCase consultarPrevisaoRiscoUseCase;
    private final ConsultarRiscoAgregadoUseCase consultarRiscoAgregadoUseCase;

    @GetMapping("/{coIbge}")
    @Operation(
            summary = "Previsao de risco epidemiologico",
            description = """
                    Calcula score de risco para as proximas 2 semanas cruzando
                    padrao sazonal historico (SINAN) com previsao climatica
                    (Open-Meteo). Baseado em evidencia cientifica publicada sobre
                    condicoes favoraveis ao Aedes aegypti.

                    Score 0-8: 0-1 Baixo · 2-3 Moderado · 4-5 Alto · 6+ Muito Alto.
                    Retorna contexto informacional baseado em dados publicos do SUS.
                    Nao realiza diagnostico, triagem clinica nem define conduta medica.
                    A decisao final permanece com o profissional de saude habilitado.
                    """)
    public PrevisaoRiscoResponse getPrevisaoRisco(
            @PathVariable String coIbge,
            @RequestParam(defaultValue = "dengue") String doenca) {
        return consultarPrevisaoRiscoUseCase.buscarPorMunicipio(coIbge);
    }

    @GetMapping("/brasil/risco-agregado")
    @Operation(
            summary = "Risco agregado para o Brasil",
            description = """
                    Calcula o risco epidemiologico agregado para todo o Brasil
                    baseado na coordenada media de todos os municipios. Retorna
                    classificacao de risco e previsao diaria para os proximos 14 dias.

                    Score 0-8: 0-1 Baixo · 2-3 Moderado · 4-5 Alto · 6+ Muito Alto.
                    """)
    public PrevisaoRiscoResponse getRiscoBrasil() {
        return consultarRiscoAgregadoUseCase.buscarBrasil();
    }

    @GetMapping("/estado/{uf}/risco-agregado")
    @Operation(
            summary = "Risco agregado para um estado",
            description = """
                    Calcula o risco epidemiologico agregado para um estado especifico
                    baseado na coordenada media de seus municipios. Retorna
                    classificacao de risco e previsao diaria para os proximos 14 dias.

                    Score 0-8: 0-1 Baixo · 2-3 Moderado · 4-5 Alto · 6+ Muito Alto.
                    """)
    public PrevisaoRiscoResponse getRiscoEstado(@PathVariable String uf) {
        return consultarRiscoAgregadoUseCase.buscarEstado(uf);
    }

    @GetMapping("/brasil/hospitais-capitais")
    @Operation(
            summary = "Hospitais das capitais do Brasil",
            description = """
                    Retorna lista de hospitais localizados nas capitais de todos
                    os estados do Brasil. Util para visualizacao e planejamento de
                    referenciais regionais.
                    """)
    public List<Estabelecimento> getHospitaisBrasil() {
        return consultarRiscoAgregadoUseCase.buscarHospitaisBrasil();
    }

    @GetMapping("/estado/{uf}/hospitais-regiao")
    @Operation(
            summary = "Hospitais da capital e regiao proxima",
            description = """
                    Retorna lista de hospitais da capital do estado mais
                    hospitais dentro de um raio de 100 km da capital.
                    Ordenados por proximidade.
                    """)
    public List<Estabelecimento> getHospitaisEstado(@PathVariable String uf) {
        return consultarRiscoAgregadoUseCase.buscarHospitaisEstado(uf);
    }
}
