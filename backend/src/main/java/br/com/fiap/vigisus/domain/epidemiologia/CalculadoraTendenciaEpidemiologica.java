package br.com.fiap.vigisus.domain.epidemiologia;

import br.com.fiap.vigisus.dto.SemanaDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CalculadoraTendenciaEpidemiologica {

    public String calcular(List<SemanaDTO> semanas) {
        if (semanas == null || semanas.size() < 8) {
            return "ESTAVEL";
        }

        List<SemanaDTO> semanasComDados = semanas.stream()
                .filter(semana -> semana.getCasos() > 0)
                .toList();

        if (semanasComDados.size() < 8) {
            return "ESTAVEL";
        }

        int quantidadeSemanas = semanasComDados.size();
        long somaUltimas4 = semanasComDados.subList(quantidadeSemanas - 4, quantidadeSemanas).stream()
                .mapToLong(SemanaDTO::getCasos)
                .sum();
        long somaAnteriores4 = semanasComDados.subList(quantidadeSemanas - 8, quantidadeSemanas - 4).stream()
                .mapToLong(SemanaDTO::getCasos)
                .sum();

        if (somaAnteriores4 == 0) {
            return "ESTAVEL";
        }

        double variacao = (double) (somaUltimas4 - somaAnteriores4) / somaAnteriores4;
        if (variacao > 0.2) {
            return "CRESCENTE";
        }
        if (variacao < -0.2) {
            return "DECRESCENTE";
        }
        return "ESTAVEL";
    }
}
