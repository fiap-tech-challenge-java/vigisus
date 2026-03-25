# Frontend (React) - Documentacao de Codigo

## 1. Objetivo do modulo
O frontend em `frontend/` apresenta os dados epidemiologicos e operacionais para usuarios tecnicos e de gestao, com foco em:
- leitura rapida de risco,
- comparacao historica,
- apoio a acao imediata,
- acessibilidade.

## 2. Arquitetura de interface

```text
App (rotas + provider de acessibilidade)
   -> Paginas (Atual / Historico)
      -> Componentes de visualizacao (cards, mapas, graficos, tabelas, IA)
         -> services/api.js (camada HTTP)
```

## 3. Mapa de codigo (arquivo por arquivo)

### 3.1 Nucleo
| Arquivo | Papel |
|---|---|
| `src/index.js` | bootstrap da aplicacao |
| `src/App.jsx` | roteamento principal e lazy loading de paginas |
| `src/index.css` | tema global, contraste, foco, estilos utilitarios |
| `src/services/api.js` | chamadas para backend |

### 3.2 Acessibilidade
| Arquivo | Papel |
|---|---|
| `src/accessibility/AccessibilityProvider.jsx` | estado global de tema, contraste e fonte com persistencia |
| `src/accessibility/AccessibilityProvider.test.jsx` | testes da camada de acessibilidade |
| `src/components/TopNav.accessibility.test.jsx` | testes de teclado/ARIA no topo |
| `src/components/MapaEstado.accessibility.test.jsx` | teste de legenda e resumo textual |
| `src/components/MapaHospitais.accessibility.test.jsx` | teste de legenda e tabela acessivel |

### 3.3 Paginas
| Arquivo | Papel |
|---|---|
| `src/pages/Atual.jsx` | dashboard situacao atual (perfil, risco, mapa, hospitais, resumo) |
| `src/pages/Historico.jsx` | dashboard historico com comparacao anual e ranking |

### 3.4 Componentes funcionais
| Arquivo | Papel |
|---|---|
| `src/components/TopNav.jsx` | filtros de consulta, navegacao entre modos e painel de acessibilidade |
| `src/components/HeaderAlerta.jsx` | cabecalho de risco sintetico |
| `src/components/OQueFazerAgora.jsx` | recomendacoes taticas por perfil de usuario |
| `src/components/StatusRapido.jsx` | indicadores compactos (incidencia/tendencia/risco) |
| `src/components/KpiCards.jsx` | cards principais de metricas |
| `src/components/KpisHistorico.jsx` | KPIs especificos do modo historico |
| `src/components/CurvaEpidemiologica.jsx` | serie temporal de semanas epidemiologicas |
| `src/components/ComparacaoAnual.jsx` | comparacao dos ultimos anos |
| `src/components/MapaEstado.jsx` | mapa regional com legenda e resumo textual acessivel |
| `src/components/MapaHospitais.jsx` | mapa de hospitais + lista + tabela acessivel |
| `src/components/TabelaRanking.jsx` | ranking territorial |
| `src/components/RiscoFuturo.jsx` | previsao de risco em 14 dias |
| `src/components/ResumoIa.jsx` | consolidacao narrativa operacional |
| `src/components/InsightsIaBloco.jsx` | bloco de insights contextuais de IA |
| `src/components/IAHistorico.jsx` | narrativa de IA no historico |
| `src/components/InterpretacaoOperacional.jsx` | interpretacao operacional orientada a decisao |

### 3.5 Utilitarios
| Arquivo | Papel |
|---|---|
| `src/utils/cores.js` | mapeamento semantico de cores/classificacoes |
| `src/utils/iaInsights.js` | regras auxiliares para texto de IA |

## 4. Como o frontend foi construido
- Componentizacao orientada a dominio de tela (nao por widget generico).
- Separacao entre:
  - estado/pagina,
  - visualizacao/componentes,
  - integracao HTTP.
- Uso de lazy loading para reduzir custo inicial.
- Controle de requisicao em transicao para evitar race condition e duplicidade de clique.

## 5. Acessibilidade implementada
- tema escuro e alto contraste;
- aumento/reducao de fonte com persistencia em `localStorage`;
- skip link para conteudo principal;
- foco visivel em navegacao por teclado;
- cards focaveis por `Tab`;
- ARIA em estados de carregamento/erro;
- mapas com:
  - legenda visual,
  - resumo textual por nivel,
  - lista de estados/cidades por classificacao,
  - tabela acessivel no contexto hospitalar.

## 6. Performance e UX
- roteamento e componentes pesados com `lazy` + `Suspense`;
- feedback explicito de busca (`Buscando...`) para evitar multi-clique;
- cancelamento de requisicoes em mudancas rapidas de filtros;
- cache em carregamento de geojson (mapas).

## 7. Validador de qualidade
- build de producao via `npm run build`;
- testes unitarios de acessibilidade via `npm test -- --watchAll=false --runInBand`.
