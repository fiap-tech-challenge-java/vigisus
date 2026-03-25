package br.com.fiap.vigisus.domain.operacional;

import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.ContextoEpidemiologicoDTO;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.PrevisaoProximosDiasDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChecklistOperacionalPolicy {

    public List<String> montarChecklist(String nivelAtencao,
                                        ContextoEpidemiologicoDTO contexto,
                                        PrevisaoProximosDiasDTO previsao) {
        return switch (nivelAtencao) {
            case "CRITICO" -> List.of(
                    "Volume de suspeitas compatÃƒÂ­vel com situaÃƒÂ§ÃƒÂ£o de surto",
                    "Contexto epidemiolÃƒÂ³gico: " + contexto.getClassificacaoAtual()
                            + " Ã¢â‚¬â€ " + contexto.getCasosUltimasSemanas() + " casos nas ÃƒÂºltimas 4 semanas",
                    "PadrÃƒÂ£o climÃƒÂ¡tico: " + previsao.getRiscoClimatico(),
                    "Hospitais de referÃƒÂªncia mais prÃƒÂ³ximos listados abaixo",
                    "Contato VigilÃƒÂ¢ncia EpidemiolÃƒÂ³gica Municipal: 0800-644-6645",
                    "Contato Central de RegulaÃƒÂ§ÃƒÂ£o MG: (31) 3916-6868"
            );
            case "ELEVADO" -> List.of(
                    "NÃƒÂºmero de suspeitas acima da mÃƒÂ©dia para o perÃƒÂ­odo",
                    "Contexto epidemiolÃƒÂ³gico: " + contexto.getClassificacaoAtual()
                            + ", tendÃƒÂªncia " + contexto.getTendencia(),
                    "Risco climÃƒÂ¡tico: " + previsao.getRiscoClimatico(),
                    "Hospitais de referÃƒÂªncia com leitos disponÃƒÂ­veis listados abaixo",
                    "PrevisÃƒÂ£o climÃƒÂ¡tica: " + previsao.getTendencia7Dias()
            );
            default -> List.of(
                    "SituaÃƒÂ§ÃƒÂ£o dentro do padrÃƒÂ£o histÃƒÂ³rico para o perÃƒÂ­odo",
                    "Contexto epidemiolÃƒÂ³gico: " + contexto.getClassificacaoAtual()
                            + ", tendÃƒÂªncia " + contexto.getTendencia(),
                    "Risco climÃƒÂ¡tico: " + previsao.getRiscoClimatico()
            );
        };
    }
}
