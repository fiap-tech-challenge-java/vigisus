package br.com.fiap.vigisus.domain.triagem;

import org.springframework.stereotype.Component;

@Component
public class PriorizacaoTriagemPolicy {

    public double resolverMultiplicador(String classificacao) {
        if (classificacao == null) {
            return 1.0;
        }
        return switch (classificacao) {
            case "EPIDEMIA" -> 1.5;
            case "ALTO" -> 1.2;
            case "BAIXO" -> 0.8;
            default -> 1.0;
        };
    }

    public String classificarPrioridade(double scoreFinal) {
        if (scoreFinal < 3) {
            return "BAIXA";
        }
        if (scoreFinal <= 5) {
            return "MEDIA";
        }
        if (scoreFinal <= 8) {
            return "ALTA";
        }
        return "CRITICA";
    }

    public String resolverCor(String prioridade) {
        return switch (prioridade) {
            case "BAIXA" -> "VERDE";
            case "MEDIA" -> "AMARELO";
            case "ALTA" -> "LARANJA";
            default -> "VERMELHO";
        };
    }

    public boolean requerObservacao(String prioridade) {
        return "ALTA".equals(prioridade) || "CRITICA".equals(prioridade);
    }
}
