package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.AdminBuscaIaDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class IaBuscaTracker {

    private static final int MAX_QUERY_LENGTH = 120;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final class Entrada {
        final AtomicLong contagem = new AtomicLong(0);
        volatile String ultimaConsulta;
    }

    private final ConcurrentHashMap<String, Entrada> entradas = new ConcurrentHashMap<>();

    public void registrar(String pergunta) {
        if (pergunta == null || pergunta.isBlank()) {
            return;
        }
        String chave = normalizar(pergunta);
        Entrada entrada = entradas.computeIfAbsent(chave, k -> new Entrada());
        entrada.contagem.incrementAndGet();
        entrada.ultimaConsulta = LocalDateTime.now().format(FORMATTER);
    }

    public List<AdminBuscaIaDTO> listarMaisFrequentes(int limite) {
        return entradas.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().contagem.get(), a.getValue().contagem.get()))
                .limit(limite)
                .map(e -> AdminBuscaIaDTO.builder()
                        .pergunta(e.getKey())
                        .contagem(e.getValue().contagem.get())
                        .ultimaConsulta(e.getValue().ultimaConsulta != null ? e.getValue().ultimaConsulta : "-")
                        .build())
                .collect(Collectors.toList());
    }

    private String normalizar(String pergunta) {
        String normalizada = pergunta.trim().toLowerCase();
        if (normalizada.length() > MAX_QUERY_LENGTH) {
            normalizada = normalizada.substring(0, MAX_QUERY_LENGTH);
        }
        return normalizada;
    }
}
