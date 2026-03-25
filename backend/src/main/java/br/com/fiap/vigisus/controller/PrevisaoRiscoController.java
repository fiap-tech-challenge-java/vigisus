package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import br.com.fiap.vigisus.service.RiscoAgregadoService;
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
@Tag(name = "Previsão de Risco")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PrevisaoRiscoController {

    private final PrevisaoRiscoService previsaoRiscoService;
    private final RiscoAgregadoService riscoAgregadoService;
    private final IaService iaService;

    @GetMapping("/{coIbge}")
    @Operation(
            summary = "Previsão de risco epidemiológico",
            description = """
                    Calcula score de risco para as próximas 2 semanas cruzando
                    padrão sazonal histórico (SINAN) com previsão climática
                    (Open-Meteo). Baseado em evidência científica publicada sobre
                    condições favoráveis ao Aedes aegypti.

                    Score 0-8: 0-1 Baixo · 2-3 Moderado · 4-5 Alto · 6+ Muito Alto.
                    Retorna contexto informacional baseado em dados públicos do SUS.
                    Não realiza diagnóstico, triagem clínica nem define conduta médica.
                    A decisão final permanece com o profissional de saúde habilitado.
                    """)
    public PrevisaoRiscoResponse getPrevisaoRisco(
            @PathVariable String coIbge,
            @RequestParam(defaultValue = "dengue") String doenca) {

        PrevisaoRiscoResponse previsao = previsaoRiscoService.calcularRisco(coIbge);
        previsao.setTextoIa(iaService.gerarTextoRisco(previsao));
        return previsao;
    }

    @GetMapping("/brasil/risco-agregado")
    @Operation(
            summary = "Risco agregado para o Brasil",
            description = """
                    Calcula o risco epidemiológico agregado para todo o Brasil
                    baseado na coordenada média de todos os municípios. Retorna
                    classificação de risco e previsão diária para os próximos 14 dias.
                    
                    Score 0-8: 0-1 Baixo · 2-3 Moderado · 4-5 Alto · 6+ Muito Alto.
                    """)
    public PrevisaoRiscoResponse getRiscoBrasil() {
        PrevisaoRiscoResponse risco = riscoAgregadoService.calcularRiscoBrasil();
        if (risco != null) {
            risco.setTextoIa(iaService.gerarTextoRisco(risco));
        }
        return risco;
    }

    @GetMapping("/estado/{uf}/risco-agregado")
    @Operation(
            summary = "Risco agregado para um estado",
            description = """
                    Calcula o risco epidemiológico agregado para um estado específico
                    baseado na coordenada média de seus municípios. Retorna
                    classificação de risco e previsão diária para os próximos 14 dias.
                    
                    Score 0-8: 0-1 Baixo · 2-3 Moderado · 4-5 Alto · 6+ Muito Alto.
                    """)
    public PrevisaoRiscoResponse getRiscoEstado(@PathVariable String uf) {
        PrevisaoRiscoResponse risco = riscoAgregadoService.calcularRiscoEstado(uf);
        if (risco != null) {
            risco.setTextoIa(iaService.gerarTextoRisco(risco));
        }
        return risco;
    }

    @GetMapping("/brasil/hospitais-capitais")
    @Operation(
            summary = "Hospitais das capitais do Brasil",
            description = """
                    Retorna lista de hospitais localizados nas capitais de todos
                    os estados do Brasil. Útil para visualização e planejamento de
                    referência regionais.
                    """)
    public List<Estabelecimento> getHospitaisBrasil() {
        return riscoAgregadoService.buscarHospitaisBrasil();
    }

    @GetMapping("/estado/{uf}/hospitais-regiao")
    @Operation(
            summary = "Hospitais da capital e região próxima",
            description = """
                    Retorna lista de hospitais da capital do estado mais
                    hospitais dentro de um raio de 100 km da capital.
                    Ordenados por proximidade.
                    """)
    public List<Estabelecimento> getHospitaisEstado(@PathVariable String uf) {
        return riscoAgregadoService.buscarHospitaisEstado(uf);
    }
}
