package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;

import java.time.Year;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IaServiceFallback implements IaService {

    private static final Pattern PATTERN_ANO = Pattern.compile("\\b(20\\d{2})\\b");
    private static final Pattern PATTERN_DOENCA = Pattern.compile(
            "\\b(dengue|chikungunya|zika)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_UF = Pattern.compile("\\b([A-Z]{2})\\b");

    @Override
    public String gerarTextoEpidemiologico(PerfilEpidemiologicoResponse perfil) {
        return String.format(
                "Em %d, %s/%s registrou %d casos de %s, " +
                "com incidência de %.1f casos por 100 mil habitantes. " +
                "A situação foi classificada como %s.",
                perfil.getAno(),
                perfil.getMunicipio(),
                perfil.getUf(),
                perfil.getTotal(),
                perfil.getDoenca(),
                perfil.getIncidencia(),
                perfil.getClassificacao());
    }

    @Override
    public String gerarTextoRisco(PrevisaoRiscoResponse previsao) {
        List<String> fatores = previsao.getFatores();
        String fator1 = (fatores != null && !fatores.isEmpty()) ? fatores.get(0) : "N/A";
        String fator2 = (fatores != null && fatores.size() > 1) ? fatores.get(1) : "N/A";
        return String.format(
                "Com base na previsão climática para os próximos 14 dias, " +
                "%s apresenta risco %s para dengue. " +
                "Principais fatores: %s, %s.",
                previsao.getMunicipio(),
                previsao.getClassificacao(),
                fator1,
                fator2);
    }

    @Override
    public IntencaoDTO interpretarPergunta(String pergunta) {
        Integer ano = null;
        String doenca = null;
        String uf = null;

        Matcher mAno = PATTERN_ANO.matcher(pergunta);
        if (mAno.find()) {
            ano = Integer.parseInt(mAno.group(1));
        }

        Matcher mDoenca = PATTERN_DOENCA.matcher(pergunta);
        if (mDoenca.find()) {
            doenca = mDoenca.group(1).toLowerCase();
        }

        Matcher mUf = PATTERN_UF.matcher(pergunta);
        if (mUf.find()) {
            uf = mUf.group(1);
        }

        return IntencaoDTO.builder()
                .municipio(null)
                .uf(uf)
                .doenca(doenca)
                .ano(ano != null ? ano : Year.now().getValue())
                .build();
    }

    @Override
    public String gerarTextoTriagem(String prioridade, List<String> sintomas, String alertaEpidemiologico) {
        return String.format(
                "Prioridade %s. Sintomas relatados: %s. %s " +
                "Oriente o profissional de saúde conforme protocolo municipal.",
                prioridade, sintomas, alertaEpidemiologico);
    }
}
