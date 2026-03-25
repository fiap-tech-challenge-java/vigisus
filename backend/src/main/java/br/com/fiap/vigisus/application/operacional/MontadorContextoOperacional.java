package br.com.fiap.vigisus.application.operacional;

import org.springframework.stereotype.Component;

@Component
public class MontadorContextoOperacional {

    public String montarContextoAtual(int suspeitasDia, String nomeMunicipio, String classificacao, String tendencia) {
        return String.format(
                "%d suspeitas registradas hoje em %s, municÃƒÂ­pio com classificaÃƒÂ§ÃƒÂ£o epidemiolÃƒÂ³gica %s" +
                        " e tendÃƒÂªncia %s nas ÃƒÂºltimas 4 semanas.",
                suspeitasDia,
                nomeMunicipio,
                classificacao,
                tendencia
        );
    }

    public String montarPadraoHistorico(String comparativoHistorico) {
        if (comparativoHistorico == null || comparativoHistorico.isBlank()) {
            return "Dados histÃƒÂ³ricos insuficientes para comparaÃƒÂ§ÃƒÂ£o do perÃƒÂ­odo.";
        }
        String comparativoNormalizado = comparativoHistorico.toLowerCase();
        if (comparativoNormalizado.contains("nao havia registros")
                || comparativoNormalizado.contains("nÃƒÂ£o havia registros")
                || comparativoNormalizado.contains("nãƒâ£o havia registros")
                || comparativoNormalizado.contains("havia registros compar")) {
            return "Dados histÃƒÂ³ricos insuficientes para comparaÃƒÂ§ÃƒÂ£o do perÃƒÂ­odo.";
        }
        return comparativoHistorico;
    }

    public String montarContextoIa(String contextoAtual, String padraoHistorico, String nivelAtencao) {
        return String.format(
                "Contexto atual: %s PadrÃƒÂ£o histÃƒÂ³rico: %s NÃƒÂ­vel de atenÃƒÂ§ÃƒÂ£o: %s",
                contextoAtual,
                padraoHistorico,
                nivelAtencao
        );
    }
}
