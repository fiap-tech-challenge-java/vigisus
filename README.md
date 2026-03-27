# VigiSUS

Plataforma de vigilancia epidemiologica para apoio a decisao em saude publica, com foco inicial em dengue, chikungunya e zika.

O projeto integra dados publicos nacionais, aplica regras de dominio no backend e entrega uma experiencia web acessivel para leitura em nivel Brasil, Estado e Municipio.

## Sumario
- [Visao do Projeto](#visao-do-projeto)
- [Objetivos](#objetivos)
- [Pontos Fortes](#pontos-fortes)
- [Arquitetura e Decisoes Tecnicas](#arquitetura-e-decisoes-tecnicas)
- [Stack e Skills Tecnicas](#stack-e-skills-tecnicas)
- [Fontes de Dados e APIs Consumidas](#fontes-de-dados-e-apis-consumidas)
- [Estrutura do Repositorio](#estrutura-do-repositorio)
- [Como Rodar](#como-rodar)
- [Qualidade e Testes](#qualidade-e-testes)
- [Endpoints Principais](#endpoints-principais)
- [Segurança](#segurança)
- [Documentacao Detalhada](#documentacao-detalhada)
- [Roadmap Tecnico](#roadmap-tecnico)

## Visao do Projeto
O VigiSUS foi desenhado como uma plataforma integrada:
- `data-pipeline/` coleta e normaliza dados publicos (ETL Python).
- `backend/` expoe APIs REST com regras epidemiologicas e operacionais (Spring Boot).
- `frontend/` entrega leitura operacional, mapas e acessibilidade (React).

O sistema foi pensado para uso real de apoio a contexto e priorizacao. Ele nao substitui diagnostico medico.

## Objetivos
- Consolidar dados publicos em uma base unica para consulta epidemiologica.
- Oferecer leitura nacional, estadual e municipal em uma interface simples.
- Apoiar tomada de decisao com risco climatico e pressao assistencial.
- Reduzir friccao operacional com busca por linguagem natural e visao consolidada.
- Manter arquitetura evolutiva com boas praticas de engenharia (DDD, Clean Architecture, SOLID, TDD).

## Pontos Fortes
- Arquitetura em camadas com separacao clara de responsabilidades.
- Modelagem de dominio com policies e calculators testaveis.
- Endpoints agregados para reduzir round-trips e melhorar first-load.
- Cache com Caffeine para reduzir custo de consultas repetidas.
- Frontend acessivel com dark mode, alto contraste, ajuste de fonte, teclado e ARIA.
- Pipeline preparado para ingestao reproduzivel e execucao automatizada.
- Suite de testes abrangente no backend, frontend e pipeline.

## Arquitetura e Decisoes Tecnicas
### Visao macro
```text
Fontes publicas (DATASUS, IBGE, Open-Meteo, OpenDataSUS)
            |
            v
     Data Pipeline (Python)
            |
            v
      PostgreSQL (dados unificados)
            |
            v
 Backend Spring Boot (API + regras de dominio + cache)
            |
            v
 Frontend React (dashboards, mapas, acessibilidade)
```

### DDD + Clean Architecture (backend)
- `controller`: entrada HTTP e contrato de API.
- `application`: use cases (orquestracao de fluxo).
- `domain`: regras puras de negocio (policies/calculators).
- `application.port`: contratos para dependencias externas.
- `infrastructure.persistence`: adaptadores de persistencia que implementam as portas.

Essa separacao facilita teste unitario, evolucao de regras e troca de infraestrutura.

### SOLID aplicado
- SRP: policies e calculators isolados por responsabilidade.
- OCP: regras de classificacao podem evoluir sem reescrever controladores.
- DIP: use cases dependem de portas, nao de implementacoes concretas.

### Performance aplicada
- Caches para perfil, ranking, risco e clima.
- Endpoint consolidado `GET /api/brasil/dashboard` para carga inicial.
- Chave de cache de ranking incluindo `uf/doenca/ano/top/ordem` para consistencia.
- Front com carregamento progressivo (dados principais primeiro, complementares depois).
- Front com lazy loading de componentes pesados.
- Pipeline CNES com conversao CSV->JSON em streaming (menor uso de memoria).

### Acessibilidade e UX
- Provider de acessibilidade persistindo preferencias no navegador.
- Tema claro/escuro, alto contraste e escala de fonte.
- Feedback de carregamento, estados de busca e mensagens ARIA.
- Navegacao por teclado e cards focaveis em pontos de leitura.

## Stack e Skills Tecnicas
| Categoria | Escolha | Motivo |
|---|---|---|
| Backend | Java 17 + Spring Boot 3.2 | Ecosistema robusto para API e regras de dominio |
| Persistencia | Spring Data JPA + PostgreSQL + Flyway | Evolucao controlada de schema e consultas estruturadas |
| Cache | Caffeine | Simples, rapido e efetivo para leitura repetida |
| IA | Gemini com fallback local | Narrativa contextual sem bloquear operacao sem chave |
| Frontend | React 18 + React Router + Tailwind + Chart.js + Leaflet | UI moderna, visualizacao e mapas |
| Pipeline | Python (requests/pandas/pysus e scripts ETL) | Alta produtividade para ingestao e transformacao |
| Testes | JUnit/Mockito, React Testing Library, unittest (Python) | Cobertura de regras, API, UI e ETL |

## Fontes de Dados e APIs Consumidas
| Fonte | Tipo | Uso no projeto |
|---|---|---|
| DATASUS / SINAN (FTP) | DBC/arquivos publicos | Casos epidemiologicos |
| DATASUS / CNES (FTP) | DBC/arquivos publicos | Estabelecimentos, leitos e servicos |
| IBGE API | REST | Dados territoriais e populacionais |
| Open-Meteo API | REST | Clima atual e previsao para risco |
| OpenDataSUS | Dados abertos | Apoio a validacoes e insumos de pipeline |
| Gemini API (opcional) | IA | Texto analitico em linguagem natural |

Observacao: se `GEMINI_API_KEY` nao estiver configurada, o backend usa fallback textual deterministico.

## Estrutura do Repositorio
```text
vigisus/
  backend/        # API Spring Boot + regras de dominio
  frontend/       # Aplicacao React
  data-pipeline/  # ETL Python e clientes de integracao
  docs/           # Documentacao tecnica detalhada
  docker-compose.yml
  .env.example
```

## Como Rodar
### Pre-requisitos
- Java 17+
- Node 18+ (recomendado Node 20)
- Python 3.10+
- Docker + Docker Compose plugin

### 1) Configurar variaveis de ambiente
```bash
cp .env.example .env
```

No Windows (PowerShell):
```powershell
Copy-Item .env.example .env
```

### 2) Subir stack completa com Docker
```bash
docker compose up --build
```

Acessos após subir:
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`

> O banco é populado automaticamente com dados de demonstração ao subir a stack.
> Para recarregar os dados: `docker compose run --rm seed`

### 3) Rodar em modo desenvolvimento local (alternativa)
1. Subir somente banco:
```bash
docker compose up -d postgres
```

2. Backend:
```bash
cd backend
./mvnw spring-boot:run
```
No Windows:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

3. Frontend:
```bash
cd frontend
npm install
npm start
```

Acesso local do frontend (CRA): `http://localhost:3000`

### 4) Rodar pipeline de dados
```bash
cd data-pipeline
pip install -r requirements.txt
python validar_apis.py
python run_all.py
```

Carga de demo rapida:
```bash
python seed_mock.py
```

Guia de demonstracao funcional:
- [docs/COMO_DEMONSTRAR.md](docs/COMO_DEMONSTRAR.md)

## Qualidade e Testes
### Backend
```bash
cd backend
./mvnw test
```
No Windows:
```powershell
cd backend
.\mvnw.cmd test
```

JaCoCo:
```bash
./mvnw test jacoco:report
```
Relatorio: `backend/target/site/jacoco/index.html`

### Frontend
```bash
cd frontend
npm test -- --watchAll=false --runInBand
npm run build
```

### Pipeline Python
```bash
python -m unittest discover -s data-pipeline/tests
python -m compileall data-pipeline
```

## Endpoints Principais
Base URL local: `http://localhost:8080`

| Metodo | Endpoint | Finalidade |
|---|---|---|
| GET | `/api/brasil/casos` | Perfil epidemiologico agregado do Brasil |
| GET | `/api/brasil/dashboard` | Payload agregado para dashboard inicial |
| GET | `/api/ranking` | Ranking municipal por incidencia |
| GET | `/api/ranking/estado-historico` | Historico agregado por estado |
| GET | `/api/perfil/{coIbge}` | Perfil epidemiologico municipal |
| GET | `/api/previsao-risco/{coIbge}` | Risco municipal |
| GET | `/api/previsao-risco/brasil/risco-agregado` | Risco agregado Brasil |
| GET | `/api/previsao-risco/estado/{uf}/risco-agregado` | Risco agregado Estado |
| GET | `/api/previsao-risco/brasil/hospitais-capitais` | Hospitais referencia Brasil |
| GET | `/api/previsao-risco/estado/{uf}/hospitais-regiao` | Hospitais por estado/regiao |
| GET | `/api/encaminhar` | Encaminhamento hospitalar por condicao |
| POST | `/api/busca` | Busca por linguagem natural |
| GET | `/api/busca/perfil-direto` | Busca direta por municipio/UF |
| POST | `/api/triagem/avaliar` | Triagem orientativa |
| GET | `/api/triagem/sintomas-validos` | Catalogo de sintomas |
| POST | `/api/operacional/pressao` | Analise de pressao operacional |
| GET | `/api/operacional/protocolo-surto` | Protocolo de surto por nivel |
| DELETE | `/api/admin/cache` | Limpeza manual de cache |

Documentacao viva da API:
- `http://localhost:8080/swagger-ui.html`

## Segurança

A API VigiSUS expõe exclusivamente **dados epidemiológicos públicos e agregados** 
provenientes do DATASUS, IBGE e Open-Meteo. Não há armazenamento ou exposição 
de dados pessoais de pacientes, estando em conformidade com a LGPD para dados 
de saúde pública.

Todos os endpoints de consulta são públicos por design. Autenticação JWT 
para operadores está planejada para a versão 2.0 (roadmap técnico).

## Documentacao Detalhada
- Arquitetura final: [docs/ARQUITETURA_FINAL_PROJETO.md](docs/ARQUITETURA_FINAL_PROJETO.md)
- Backend (codigo): [docs/CODIGO_BACKEND.md](docs/CODIGO_BACKEND.md)
- Frontend (codigo): [docs/CODIGO_FRONTEND.md](docs/CODIGO_FRONTEND.md)
- Data pipeline (codigo): [docs/CODIGO_DATA_PIPELINE.md](docs/CODIGO_DATA_PIPELINE.md)
- Guia completo de estudo: [docs/CODIGO_PROJETO_COMPLETO.md](docs/CODIGO_PROJETO_COMPLETO.md)
- Roteiro de demo: [docs/COMO_DEMONSTRAR.md](docs/COMO_DEMONSTRAR.md)
- Resumo executivo do hackathon: [docs/hackathon-sus-resumo.md](docs/hackathon-sus-resumo.md)
- Guia do pipeline: [data-pipeline/README.md](data-pipeline/README.md)

## Roadmap Tecnico
- Consolidar uso de `GET /api/brasil/dashboard` no front para reduzir ainda mais latencia inicial.
- Evoluir observabilidade com metricas de endpoint e cache hit ratio.
- Expandir cobertura automatizada no frontend para fluxos de paginas completas.
- Avancar testes de contrato API (consumer/provider).
- Evoluir documentacao para padrao ADR (Architecture Decision Records).

---

Projeto academico aplicado a contexto de inovacao em saude publica.

