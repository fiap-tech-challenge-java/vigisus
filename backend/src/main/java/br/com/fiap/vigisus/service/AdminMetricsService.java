package br.com.fiap.vigisus.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coleta e expõe métricas operacionais do VigiSUS para o dashboard administrativo.
 * Os contadores são mantidos em memória e também registrados no MeterRegistry
 * para exposição via /actuator/metrics na porta 9090.
 */
@Service
public class AdminMetricsService {

    private final Counter buscasTotalCounter;
    private final Counter buscasIaCounter;
    private final Counter triagensCounter;
    private final Counter cacheHitsCounter;
    private final MeterRegistry meterRegistry;

    private final AtomicLong buscasTotal = new AtomicLong(0);
    private final AtomicLong buscasIa = new AtomicLong(0);
    private final AtomicLong triagens = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);

    private final ConcurrentHashMap<String, AtomicLong> contagemMunicipios = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> contagemEstados = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> contagemPerguntasIa = new ConcurrentHashMap<>();

    public AdminMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.buscasTotalCounter = Counter.builder("vigisus.buscas.total")
                .description("Total de buscas realizadas")
                .register(meterRegistry);
        this.buscasIaCounter = Counter.builder("vigisus.buscas.ia")
                .description("Buscas que utilizaram interpretação por IA")
                .register(meterRegistry);
        this.triagensCounter = Counter.builder("vigisus.triagens.total")
                .description("Total de triagens realizadas")
                .register(meterRegistry);
        this.cacheHitsCounter = Counter.builder("vigisus.cache.hits")
                .description("Total de acertos de cache")
                .register(meterRegistry);
    }

    public void registrarBusca(String municipio, String sgUf) {
        buscasTotalCounter.increment();
        buscasTotal.incrementAndGet();
        if (municipio != null && !municipio.isBlank()) {
            String chave = municipio.trim() + (sgUf != null ? "/" + sgUf.trim().toUpperCase() : "");
            contagemMunicipios.computeIfAbsent(chave, k -> new AtomicLong(0)).incrementAndGet();
            Counter.builder("vigisus.buscas.municipio")
                    .description("Buscas por município")
                    .tag("municipio", municipio.trim())
                    .tag("uf", sgUf != null ? sgUf.trim().toUpperCase() : "")
                    .register(meterRegistry)
                    .increment();
            if (sgUf != null && !sgUf.isBlank()) {
                contagemEstados.computeIfAbsent(sgUf.trim().toUpperCase(), k -> new AtomicLong(0)).incrementAndGet();
            }
        }
    }

    public void registrarBuscaIa(String pergunta) {
        buscasIaCounter.increment();
        buscasIa.incrementAndGet();
        if (pergunta != null && !pergunta.isBlank()) {
            String chave = pergunta.trim().toLowerCase();
            contagemPerguntasIa.computeIfAbsent(chave, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    public void registrarTriagem() {
        triagensCounter.increment();
        triagens.incrementAndGet();
    }

    public void registrarCacheHit() {
        cacheHitsCounter.increment();
        cacheHits.incrementAndGet();
    }

    public long getBuscasTotal() {
        return buscasTotal.get();
    }

    public long getBuscasIa() {
        return buscasIa.get();
    }

    public long getTriagens() {
        return triagens.get();
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public List<Map<String, Object>> getTopMunicipios(int top) {
        return rankearContagem(contagemMunicipios, top);
    }

    public List<Map<String, Object>> getTopEstados(int top) {
        return rankearContagem(contagemEstados, top);
    }

    public List<Map<String, Object>> getTopPerguntasIa(int top) {
        return rankearContagem(contagemPerguntasIa, top);
    }

    private List<Map<String, Object>> rankearContagem(ConcurrentHashMap<String, AtomicLong> contagem, int top) {
        List<Map.Entry<String, AtomicLong>> entries = new ArrayList<>(contagem.entrySet());
        entries.sort(Comparator.comparingLong((Map.Entry<String, AtomicLong> e) -> e.getValue().get()).reversed());

        List<Map<String, Object>> resultado = new ArrayList<>();
        int limite = Math.min(top, entries.size());
        for (int i = 0; i < limite; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nome", entries.get(i).getKey());
            item.put("total", entries.get(i).getValue().get());
            item.put("posicao", i + 1);
            resultado.add(Collections.unmodifiableMap(item));
        }
        return Collections.unmodifiableList(resultado);
    }
}
