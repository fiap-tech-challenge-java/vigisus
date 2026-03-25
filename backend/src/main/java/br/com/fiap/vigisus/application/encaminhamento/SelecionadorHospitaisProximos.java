package br.com.fiap.vigisus.application.encaminhamento;

import br.com.fiap.vigisus.domain.geografia.CalculadoraDistanciaGeografica;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.ServicoEspecializado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SelecionadorHospitaisProximos {

    private static final Set<String> SERVICOS_INFECCIOSAS = Set.of("135", "136");

    private final CalculadoraDistanciaGeografica calculadoraDistanciaGeografica;

    public List<HospitalDTO> selecionar(double latOrigem, double lonOrigem,
                                        List<Leito> leitos,
                                        Map<String, Estabelecimento> estabelecimentosPorCnes,
                                        Map<String, List<ServicoEspecializado>> servicosPorCnes,
                                        int raioKm,
                                        int limite) {
        List<HospitalDTO> resultado = new ArrayList<>();

        for (Leito leito : leitos) {
            Estabelecimento estabelecimento = estabelecimentosPorCnes.get(leito.getCoCnes());
            if (estabelecimento == null
                    || estabelecimento.getNuLatitude() == null
                    || estabelecimento.getNuLongitude() == null) {
                continue;
            }

            double distancia = calculadoraDistanciaGeografica.haversine(
                    latOrigem,
                    lonOrigem,
                    estabelecimento.getNuLatitude(),
                    estabelecimento.getNuLongitude()
            );
            if (distancia > raioKm) {
                continue;
            }

            boolean temInfecciosas = servicosPorCnes.getOrDefault(leito.getCoCnes(), List.of()).stream()
                    .anyMatch(servico -> SERVICOS_INFECCIOSAS.contains(servico.getServEsp()));

            resultado.add(HospitalDTO.builder()
                    .coCnes(estabelecimento.getCoCnes())
                    .noFantasia(estabelecimento.getNoFantasia())
                    .coMunicipio(estabelecimento.getCoMunicipio())
                    .nuTelefone(estabelecimento.getNuTelefone())
                    .qtLeitosSus(leito.getQtSus())
                    .distanciaKm(Math.round(distancia * 10.0) / 10.0)
                    .servicoInfectologia(temInfecciosas)
                    .nuLatitude(estabelecimento.getNuLatitude())
                    .nuLongitude(estabelecimento.getNuLongitude())
                    .build());
        }

        resultado.sort(Comparator.comparingDouble(HospitalDTO::getDistanciaKm));
        return resultado.stream().limit(limite).toList();
    }
}
