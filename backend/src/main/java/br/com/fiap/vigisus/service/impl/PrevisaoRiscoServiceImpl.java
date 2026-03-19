package br.com.fiap.vigisus.service.impl;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.exception.NotFoundException;
import br.com.fiap.vigisus.model.CasoDengue;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrevisaoRiscoServiceImpl implements PrevisaoRiscoService {

    private final MunicipioRepository municipioRepository;
    private final CasoDengueRepository casoDengueRepository;

    @Override
    public PrevisaoRiscoResponse calcularRisco(String coIbge, String doenca) {
        Municipio municipio = municipioRepository.findByCoIbge(coIbge)
                .orElseThrow(() -> new NotFoundException("Município não encontrado: " + coIbge));

        int anoAtual = LocalDate.now().getYear();
        List<CasoDengue> casosAtuais = casoDengueRepository.findByCoMunicipioAndAno(coIbge, anoAtual);
        List<CasoDengue> casosAnoAnterior = casoDengueRepository.findByCoMunicipioAndAno(coIbge, anoAtual - 1);

        long totalAtual = casosAtuais.stream()
                .mapToLong(c -> c.getTotalCasos() != null ? c.getTotalCasos() : 0L)
                .sum();
        long totalAnterior = casosAnoAnterior.stream()
                .mapToLong(c -> c.getTotalCasos() != null ? c.getTotalCasos() : 0L)
                .sum();

        double score = calcularScore(totalAtual, totalAnterior, municipio.getPopulacao());
        String nivel = determinarNivel(score);

        return PrevisaoRiscoResponse.builder()
                .coIbge(coIbge)
                .nomeMunicipio(municipio.getNoMunicipio())
                .doenca(doenca)
                .nivelRisco(nivel)
                .scoreRisco(score)
                .build();
    }

    private double calcularScore(long totalAtual, long totalAnterior, Long populacao) {
        if (populacao == null || populacao == 0) {
            return 0.0;
        }
        double incidencia = (double) totalAtual / populacao * 100_000;
        double variacao = totalAnterior > 0 ? (double) (totalAtual - totalAnterior) / totalAnterior : 0.0;
        return Math.min(1.0, (incidencia / 300 + Math.max(0.0, variacao)) / 2);
    }

    private String determinarNivel(double score) {
        if (score >= 0.75) return "crítico";
        if (score >= 0.50) return "alto";
        if (score >= 0.25) return "médio";
        return "baixo";
    }
}
