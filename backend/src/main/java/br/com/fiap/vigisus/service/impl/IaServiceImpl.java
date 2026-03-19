package br.com.fiap.vigisus.service.impl;

import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.service.IaService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class IaServiceImpl implements IaService {

    @Override
    public String gerarTextoEpidemiologico(PerfilEpidemiologicoResponse perfil) {
        return String.format(
                "Em %d, o município de %s (%s) registrou %d casos de %s.",
                perfil.getAno(),
                perfil.getNomeMunicipio(),
                perfil.getSgUf(),
                perfil.getTotalCasos(),
                perfil.getDoenca()
        );
    }

    @Override
    public String gerarTextoRisco(PrevisaoRiscoResponse previsao) {
        return String.format(
                "O risco epidemiológico para %s em %s é %s (score: %.2f).",
                previsao.getDoenca(),
                previsao.getNomeMunicipio(),
                previsao.getNivelRisco(),
                previsao.getScoreRisco()
        );
    }

    @Override
    public IntencaoDTO interpretarPergunta(String pergunta) {
        IntencaoDTO intencao = new IntencaoDTO();
        intencao.setDoenca("dengue");
        intencao.setAno(LocalDate.now().getYear());

        if (pergunta == null || pergunta.isBlank()) {
            return intencao;
        }

        String[] tokens = pergunta.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.equalsIgnoreCase("em") && i + 1 < tokens.length) {
                intencao.setMunicipio(tokens[i + 1]);
            }
            if (token.matches("[A-Za-z]{2}") && !token.equalsIgnoreCase("em")) {
                intencao.setUf(token.toUpperCase());
            }
            if (token.matches("\\d{4}")) {
                intencao.setAno(Integer.parseInt(token));
            }
        }

        for (String keyword : new String[]{"dengue", "chikungunya", "zika", "malaria", "leptospirose"}) {
            if (pergunta.toLowerCase().contains(keyword)) {
                intencao.setDoenca(keyword);
                break;
            }
        }

        return intencao;
    }
}
