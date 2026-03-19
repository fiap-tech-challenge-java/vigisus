package br.com.fiap.vigisus.service.impl;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.exception.NotFoundException;
import br.com.fiap.vigisus.model.CasoDengue;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import br.com.fiap.vigisus.service.PerfilEpidemiologicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfilEpidemiologicoServiceImpl implements PerfilEpidemiologicoService {

    private final MunicipioRepository municipioRepository;
    private final CasoDengueRepository casoDengueRepository;

    @Override
    public PerfilEpidemiologicoResponse gerarPerfil(String coIbge, String doenca, Integer ano) {
        Municipio municipio = municipioRepository.findByCoIbge(coIbge)
                .orElseThrow(() -> new NotFoundException("Município não encontrado: " + coIbge));

        List<CasoDengue> casos = casoDengueRepository.findByCoMunicipioAndAno(coIbge, ano);

        long totalCasos = casos.stream()
                .mapToLong(c -> c.getTotalCasos() != null ? c.getTotalCasos() : 0L)
                .sum();

        Map<Integer, Long> casosPorSemana = casos.stream()
                .collect(Collectors.toMap(
                        CasoDengue::getSemanaEpi,
                        c -> c.getTotalCasos() != null ? c.getTotalCasos() : 0L,
                        Long::sum
                ));

        return PerfilEpidemiologicoResponse.builder()
                .coIbge(coIbge)
                .nomeMunicipio(municipio.getNoMunicipio())
                .sgUf(municipio.getSgUf())
                .doenca(doenca)
                .ano(ano)
                .totalCasos(totalCasos)
                .casosPorSemana(casosPorSemana)
                .build();
    }
}
