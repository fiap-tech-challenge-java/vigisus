package br.com.fiap.vigisus.service.impl;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.HospitalDTO;
import br.com.fiap.vigisus.exception.NotFoundException;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.EstabelecimentoRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import br.com.fiap.vigisus.repository.ServicoEspecializadoRepository;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EncaminhamentoServiceImpl implements EncaminhamentoService {

    private final MunicipioRepository municipioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ServicoEspecializadoRepository servicoEspecializadoRepository;

    @Override
    public EncaminhamentoResponse buscarHospitais(String coIbge, String condicao, String gravidade) {
        Municipio municipio = municipioRepository.findByCoIbge(coIbge)
                .orElseThrow(() -> new NotFoundException("Município não encontrado: " + coIbge));

        List<HospitalDTO> hospitais = buscarHospitaisPorRaio(coIbge, null, 300, "74");

        return EncaminhamentoResponse.builder()
                .municipioOrigem(municipio.getNoMunicipio())
                .condicao(condicao)
                .gravidade(gravidade)
                .hospitais(hospitais)
                .build();
    }

    @Override
    public List<HospitalDTO> buscarHospitaisPorRaio(String coIbge, String servico, Integer raioKm, String tpLeito) {
        Municipio municipio = municipioRepository.findByCoIbge(coIbge)
                .orElseThrow(() -> new NotFoundException("Município não encontrado: " + coIbge));

        List<Estabelecimento> estabelecimentos;

        if (servico != null && !servico.isBlank()) {
            List<String> cnesComServico = servicoEspecializadoRepository.findCoCnesByServEsp(servico);
            Set<String> cnesSet = Set.copyOf(cnesComServico);
            estabelecimentos = estabelecimentoRepository.findByCoCnesIn(cnesSet);
        } else {
            estabelecimentos = estabelecimentoRepository.findAll();
        }

        return estabelecimentos.stream()
                .filter(e -> e.getNuLatitude() != null && e.getNuLongitude() != null
                        && municipio.getNuLatitude() != null && municipio.getNuLongitude() != null)
                .map(e -> {
                    double distancia = calcularDistanciaKm(
                            municipio.getNuLatitude(), municipio.getNuLongitude(),
                            e.getNuLatitude(), e.getNuLongitude());
                    return HospitalDTO.builder()
                            .coCnes(e.getCoCnes())
                            .noFantasia(e.getNoFantasia())
                            .municipio(e.getCoMunicipio())
                            .telefone(e.getNuTelefone())
                            .distanciaKm(distancia)
                            .latitude(e.getNuLatitude())
                            .longitude(e.getNuLongitude())
                            .build();
                })
                .filter(h -> h.getDistanciaKm() <= raioKm)
                .sorted(Comparator.comparingDouble(HospitalDTO::getDistanciaKm))
                .collect(Collectors.toList());
    }

    private double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6_371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
