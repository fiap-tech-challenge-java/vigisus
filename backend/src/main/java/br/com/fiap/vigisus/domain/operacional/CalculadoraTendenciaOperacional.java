package br.com.fiap.vigisus.domain.operacional;

import org.springframework.stereotype.Component;

@Component
public class CalculadoraTendenciaOperacional {

    public String calcular(long casosAtual, long casosSemana3Atras) {
        if (casosSemana3Atras == 0) {
            return casosAtual > 0 ? "CRESCENTE" : "ESTAVEL";
        }
        if (casosAtual > casosSemana3Atras * 1.2) {
            return "CRESCENTE";
        }
        if (casosAtual < casosSemana3Atras * 0.8) {
            return "DECRESCENTE";
        }
        return "ESTAVEL";
    }
}
