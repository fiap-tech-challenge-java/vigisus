package br.com.fiap.vigisus.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard de monitoramento operacional — acesso restrito (requer autenticação).
 * Disponível na porta de gestão (9090) e oculto do Swagger público.
 *
 * @Hidden oculta do Swagger público.
 */
@Hidden
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final MeterRegistry meterRegistry;

    /** Retorna top municípios mais buscados (nome, UF e contagem de buscas) */
    @GetMapping("/top-municipios")
    public List<Map<String, Object>> getTopMunicipios() {
        return Search.in(meterRegistry)
                .name("busca.municipio")
                .counters()
                .stream()
                .map(counter -> {
                    String municipio = counter.getId().getTag("municipio");
                    String uf = counter.getId().getTag("uf");
                    long buscas = (long) counter.count();
                    return Map.<String, Object>of(
                            "municipio", municipio != null ? municipio : "",
                            "uf", uf != null ? uf : "",
                            "buscas", buscas);
                })
                .sorted(Comparator.comparingLong(m -> -((Long) m.get("buscas"))))
                .limit(10)
                .collect(Collectors.toList());
    }
}
