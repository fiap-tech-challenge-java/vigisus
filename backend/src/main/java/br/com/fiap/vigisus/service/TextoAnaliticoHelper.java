package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.SemanaDTO;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class TextoAnaliticoHelper {

    private TextoAnaliticoHelper() {}

    static String montarTextoEpidemiologico(PerfilEpidemiologicoResponse perfil) {
        String local = String.format("%s/%s", safe(perfil.getMunicipio(), "Local"), safe(perfil.getUf(), "BR"));
        long totalAnterior = somarCasos(perfil.getSemanasAnoAnterior());
        Double variacao = calcularVariacao(perfil.getTotal(), totalAnterior);
        SemanaDTO pico = buscarPico(perfil.getSemanas());
        Double participacaoPico = (pico != null && perfil.getTotal() > 0)
                ? (pico.getCasos() * 100.0) / perfil.getTotal()
                : null;

        StringBuilder texto = new StringBuilder();
        texto.append(String.format(Locale.US,
                "%s registrou %d casos de %s em %d, com incidencia de %.1f por 100 mil habitantes e classificacao %s. ",
                local,
                perfil.getTotal(),
                safe(perfil.getDoenca(), "dengue"),
                perfil.getAno(),
                perfil.getIncidencia(),
                safe(perfil.getClassificacao(), "SEM_DADO").toLowerCase(Locale.ROOT)));

        if (variacao != null) {
            texto.append(String.format(Locale.US,
                    "Na comparacao com %d, o volume ficou %.1f%% %s. ",
                    perfil.getAno() - 1,
                    Math.abs(variacao),
                    variacao >= 0 ? "acima" : "abaixo"));
        }

        if (pico != null && participacaoPico != null) {
            texto.append(String.format(Locale.US,
                    "O pico ocorreu na semana %d, com %d casos, concentrando %.1f%% do total anual. ",
                    pico.getSemanaEpi(),
                    pico.getCasos(),
                    participacaoPico));
        }

        if (perfil.getTendencia() != null) {
            texto.append(String.format("A tendencia das semanas mais recentes foi classificada como %s. ",
                    perfil.getTendencia().toLowerCase(Locale.ROOT)));
        }

        if (perfil.getPosicaoEstado() != null && !perfil.getPosicaoEstado().isBlank()) {
            texto.append(String.format("No recorte estadual, %s. ", perfil.getPosicaoEstado()));
        }

        if (perfil.getIncidenciaMediaEstado() != null) {
            texto.append(String.format(Locale.US,
                    "A incidencia media dos demais municipios do estado ficou em %.1f por 100 mil, o que ajuda a dimensionar o desvio local. ",
                    perfil.getIncidenciaMediaEstado()));
        }

        texto.append("A leitura combina magnitude, ritmo e concentracao temporal, nao apenas o acumulado final.");
        return texto.toString().trim();
    }

    static String montarTextoRisco(PrevisaoRiscoResponse previsao) {
        String fatores = (previsao.getFatores() != null && !previsao.getFatores().isEmpty())
                ? String.join("; ", previsao.getFatores().stream().limit(3).toList())
                : "fatores climaticos nao detalhados";

        return String.format(Locale.US,
                "%s apresenta risco %s para as proximas 2 semanas, com score %d/8. " +
                "Os fatores predominantes observados foram: %s. " +
                "Esse texto resume o sinal prospectivo para complementar a leitura dos casos ja observados.",
                safe(previsao.getMunicipio(), "O territorio"),
                safe(previsao.getClassificacao(), "SEM_DADO").toLowerCase(Locale.ROOT),
                previsao.getScore(),
                fatores);
    }

    static String montarTextoOperacional(String contexto) {
        return "Leitura operacional consolidada: " + contexto +
                " O objetivo deste resumo e destacar pressao assistencial, tendencia e contexto territorial em uma narrativa unica.";
    }

    static long somarCasos(List<SemanaDTO> semanas) {
        if (semanas == null) return 0L;
        return semanas.stream().mapToLong(SemanaDTO::getCasos).sum();
    }

    private static Double calcularVariacao(long atual, long anterior) {
        if (anterior <= 0) return null;
        return ((double) atual - anterior) / anterior * 100.0;
    }

    private static SemanaDTO buscarPico(List<SemanaDTO> semanas) {
        if (semanas == null || semanas.isEmpty()) return null;
        return semanas.stream().max(Comparator.comparingInt(SemanaDTO::getCasos)).orElse(null);
    }

    private static String safe(String valor, String fallback) {
        return valor == null || valor.isBlank() ? fallback : valor;
    }
}
