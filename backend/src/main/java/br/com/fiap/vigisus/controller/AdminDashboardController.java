package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.service.AdminMetricsService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard administrativo do VigiSUS.
 *
 * <p>Este controller é servido exclusivamente na porta 9090
 * (management.server.port), nunca exposto na porta pública 8080.
 * A anotação @Hidden impede que apareça no Swagger público.
 */
@Hidden
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminMetricsService adminMetricsService;

    @GetMapping("/resumo")
    public ResponseEntity<Map<String, Object>> resumo() {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("buscas_total", adminMetricsService.getBuscasTotal());
        kpis.put("buscas_ia", adminMetricsService.getBuscasIa());
        kpis.put("triagens", adminMetricsService.getTriagens());
        kpis.put("cache_hits", adminMetricsService.getCacheHits());
        kpis.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(kpis);
    }

    @GetMapping("/top-municipios")
    public ResponseEntity<List<Map<String, Object>>> topMunicipios(
            @RequestParam(defaultValue = "10") int top) {
        return ResponseEntity.ok(adminMetricsService.getTopMunicipios(top));
    }

    @GetMapping("/top-estados")
    public ResponseEntity<List<Map<String, Object>>> topEstados(
            @RequestParam(defaultValue = "10") int top) {
        return ResponseEntity.ok(adminMetricsService.getTopEstados(top));
    }

    @GetMapping("/municipios-risco")
    public ResponseEntity<List<Map<String, Object>>> municipiosRisco(
            @RequestParam(defaultValue = "50") int top) {
        return ResponseEntity.ok(adminMetricsService.getTopMunicipios(top));
    }

    @GetMapping("/buscas-ia")
    public ResponseEntity<List<Map<String, Object>>> buscasIa(
            @RequestParam(defaultValue = "20") int top) {
        return ResponseEntity.ok(adminMetricsService.getTopPerguntasIa(top));
    }

    @GetMapping(value = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> dashboard() {
        String html = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>VigiSUS — Dashboard Admin</title>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 0; background: #f4f6f9; color: #333; }
                    header { background: #0a6e3f; color: white; padding: 16px 24px; }
                    header h1 { margin: 0; font-size: 1.5rem; }
                    .container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
                    .cards { display: flex; flex-wrap: wrap; gap: 16px; margin-bottom: 32px; }
                    .card { background: white; border-radius: 8px; box-shadow: 0 2px 6px rgba(0,0,0,.1);
                            padding: 20px 24px; flex: 1; min-width: 180px; }
                    .card h2 { margin: 0 0 8px; font-size: .85rem; color: #666; text-transform: uppercase; }
                    .card .value { font-size: 2rem; font-weight: bold; color: #0a6e3f; }
                    table { width: 100%; border-collapse: collapse; background: white;
                            border-radius: 8px; overflow: hidden;
                            box-shadow: 0 2px 6px rgba(0,0,0,.1); margin-bottom: 32px; }
                    th { background: #0a6e3f; color: white; padding: 10px 14px; text-align: left; font-size: .85rem; }
                    td { padding: 10px 14px; border-bottom: 1px solid #eee; font-size: .9rem; }
                    tr:last-child td { border-bottom: none; }
                    tr:hover td { background: #f0faf5; }
                    h3 { color: #0a6e3f; margin-bottom: 8px; }
                    .refresh { float: right; background: #0a6e3f; color: white; border: none;
                               padding: 8px 16px; border-radius: 4px; cursor: pointer; font-size: .85rem; }
                    .refresh:hover { background: #085c34; }
                    footer { text-align: center; padding: 24px; color: #999; font-size: .8rem; }
                  </style>
                </head>
                <body>
                  <header>
                    <h1>&#128200; VigiSUS — Dashboard Administrativo</h1>
                  </header>
                  <div class="container">
                    <button class="refresh" onclick="location.reload()">&#8635; Atualizar</button>
                    <br><br>
                    <div class="cards" id="kpis">Carregando KPIs...</div>
                    <h3>&#127970; Top Municípios Consultados</h3>
                    <table id="tbl-municipios">
                      <thead><tr><th>#</th><th>Município</th><th>Consultas</th></tr></thead>
                      <tbody>Carregando...</tbody>
                    </table>
                    <h3>&#127463;&#127479; Top Estados Consultados</h3>
                    <table id="tbl-estados">
                      <thead><tr><th>#</th><th>Estado</th><th>Consultas</th></tr></thead>
                      <tbody>Carregando...</tbody>
                    </table>
                    <h3>&#129302; Perguntas Frequentes (IA)</h3>
                    <table id="tbl-ia">
                      <thead><tr><th>#</th><th>Pergunta</th><th>Frequência</th></tr></thead>
                      <tbody>Carregando...</tbody>
                    </table>
                  </div>
                  <footer>VigiSUS &mdash; Plataforma de Vigilância Epidemiológica do SUS &mdash; Admin Port 9090</footer>
                  <script>
                    async function carregarKpis() {
                      const r = await fetch('/admin/resumo');
                      const d = await r.json();
                      document.getElementById('kpis').innerHTML = [
                        ['Buscas Total', d.buscas_total],
                        ['Buscas com IA', d.buscas_ia],
                        ['Triagens', d.triagens],
                        ['Cache Hits', d.cache_hits],
                      ].map(([t,v]) => `<div class="card"><h2>${t}</h2><div class="value">${v}</div></div>`).join('');
                    }
                    async function carregarTabela(url, id, colunas) {
                      const r = await fetch(url);
                      const rows = await r.json();
                      const tbody = document.querySelector(`#${id} tbody`);
                      if (!rows.length) { tbody.innerHTML = '<tr><td colspan="3" style="color:#999">Sem dados ainda</td></tr>'; return; }
                      tbody.innerHTML = rows.map(row =>
                        `<tr>${colunas.map(c => `<td>${row[c] ?? ''}</td>`).join('')}</tr>`
                      ).join('');
                    }
                    carregarKpis();
                    carregarTabela('/admin/top-municipios?top=10', 'tbl-municipios', ['posicao','nome','total']);
                    carregarTabela('/admin/top-estados?top=10', 'tbl-estados', ['posicao','nome','total']);
                    carregarTabela('/admin/buscas-ia?top=20', 'tbl-ia', ['posicao','nome','total']);
                  </script>
                </body>
                </html>
                """;
        return ResponseEntity.ok(html);
    }
}
