package br.com.fiap.vigisus.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Dashboard", description = "Resumo operacional e métricas de uso da API")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final MeterRegistry meterRegistry;

    @GetMapping("/resumo")
    @Operation(summary = "Resumo geral de uso da API com contadores de buscas")
    public Map<String, Object> getResumo() {
        Counter counter = meterRegistry.find("vigisus.buscas.total").counter();
        double buscasTotal = counter != null ? counter.count() : 0.0;
        return Map.of(
                "buscas_total", buscasTotal,
                "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/top-municipios")
    @Operation(summary = "Top municípios mais consultados por volume de buscas")
    public List<Map<String, Object>> getTopMunicipios() {
        return meterRegistry.find("vigisus.buscas.municipio")
                .counters()
                .stream()
                .sorted(Comparator.comparingDouble(Counter::count).reversed())
                .map(c -> Map.<String, Object>of(
                        "municipio", String.valueOf(c.getId().getTag("municipio")),
                        "total", c.count()
                ))
                .collect(Collectors.toList());
    }
}
