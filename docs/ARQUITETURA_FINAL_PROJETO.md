# Arquitetura Final do Projeto VigiSUS

## 1. Visao geral
O VigiSUS foi construido como uma plataforma integrada para vigilancia epidemiologica com foco em dengue, unindo:
- coleta/normalizacao de dados publicos (Python ETL),
- processamento e exposicao de regras de negocio (Spring Boot),
- visualizacao e apoio a decisao em interface web (React).

Arquitetura macro:

```text
Fontes publicas (IBGE, DATASUS, Open-Meteo, OpenDataSUS)
                |
                v
      Data Pipeline (Python ETL)
                |
                v
         PostgreSQL (base unica)
                |
                v
      Backend API (Spring Boot)
                |
                v
        Frontend Web (React)
```

## 2. Componentes principais

### 2.1 Data Pipeline (Python)
- Responsavel por ingestao, limpeza, normalizacao e carga em banco.
- Scripts organizados em:
  - orquestracao (`run_all.py`);
  - ingestoes por dominio (`ingest_municipios.py`, `ingest_populacao.py`, `ingest_planilhas_saude.py`, etc.);
  - clientes de integracao externa (`clients/*`).

### 2.2 Backend (Spring Boot)
- API REST com regras epidemiologicas, risco, encaminhamento e triagem.
- Organizacao em camadas alinhada a DDD + Clean Architecture:
  - `domain`: politicas e regras puras;
  - `application`: use cases;
  - `application.port`: contratos de dependencia;
  - `infrastructure.persistence`: adapters JPA;
  - `controller`: entrada HTTP;
  - `service`: camada de compatibilidade/orquestracao legada;
  - `dto`, `exception`, `config`.

### 2.3 Frontend (React)
- SPA com rotas de consulta atual e historica.
- Componentizacao por blocos funcionais (mapa, risco, ranking, cards, IA).
- Camada de acessibilidade dedicada (tema, contraste, fonte, navegacao teclado, ARIA).

## 3. Fluxos ponta a ponta

### 3.1 Carga de dados
1. Pipeline consulta APIs/fontes.
2. Pipeline transforma dados para esquema do banco.
3. Pipeline grava dados com estrategias de recarga segura (truncate + reload controlado, quando aplicavel).

### 3.2 Consulta epidemiologica
1. Frontend chama endpoint backend (ex.: perfil, ranking, risco).
2. Backend compoe dados de repositorios + regras de dominio.
3. Backend retorna DTOs para visualizacao.
4. Frontend renderiza painis, mapas e resumos IA.

### 3.3 Encaminhamento operacional
1. Frontend solicita encaminhamento.
2. Backend aplica regras de pressao SUS, distancia geografica e tipo de leito.
3. API retorna hospitais priorizados + justificativa operacional.

## 4. Padroes arquiteturais e de codigo

### 4.1 DDD
- Linguagem de dominio explicita (epidemiologia, risco, encaminhamento, triagem).
- Regras encapsuladas em politicas/calculadoras no pacote `domain`.
- Objetos e contratos orientados ao dominio no pacote `application`.

### 4.2 Clean Architecture
- Use cases desacoplados de infraestrutura via portas (`application.port`).
- Adaptadores concretos de persistencia em `infrastructure.persistence`.
- Controllers delegam para casos de uso/servicos, evitando regra de negocio na borda HTTP.

### 4.3 SOLID (evolucao pratica)
- SRP: classes de regra com responsabilidade unica.
- OCP: novas estrategias de classificacao podem ser adicionadas sem alterar fluxos centrais.
- DIP: use cases dependem de interfaces (ports), nao de repositorios concretos.

### 4.4 TDD/BDD
- Backend com ampla suite de testes de:
  - dominio,
  - aplicacao (use cases),
  - controllers,
  - servicos/config/exceptions.
- Cobertura observada no JaCoCo:
  - line coverage: 91.87% (arquivo `backend/target/site/jacoco/jacoco.csv`).
  - branch coverage: 80% (politicas de dominio cobertas com @ParameterizedTest)
- `lombok.config` configurado para excluir codigo gerado da analise de cobertura.

## 5. Design Patterns aplicados

| Pattern | Onde aplicado |
|---------|---------------|
| Strategy | IaService → GeminiImpl / FallbackImpl |
| Adapter | CasoDengueJpaAdapter, MunicipioJpaAdapter, RedeAssistencialJpaAdapter |
| Repository | Spring Data JPA + Ports como contratos de dominio |
| Factory Method | IaConfig selecionando implementacao de IA por ambiente |
| Cache-Aside | Caffeine em perfil, ranking, risco e clima |
| Policy Object | *Policy.java — regras de negocio nomeadas e testaveis |
| Value Object | CoIbge, IncidenciaPor100kHab, ScoreRisco, NivelPressaoSus |
| Domain Event | RiscoAltoDetectadoEvent publicado pelo use case de risco |
| ETL Pipeline | data-pipeline/ com extract → transform → load por dominio |

## 6. Decisoes de performance
- Pipeline:
  - sessao HTTP reutilizavel com retry e pool de conexoes;
  - carga de planilhas grandes em chunks;
  - atualizacao de populacao em lote (batch update);
  - cache de listagem FTP para evitar round-trips repetidos.
- Frontend:
  - lazy loading de paginas/componentes;
  - cancelamento de requisicoes e controle de estado de loading;
  - feedback de busca para evitar multi-clique.

## 7. Acessibilidade e UX (frontend)
- Tema escuro e alto contraste.
- Escala de fonte persistida.
- Skip link para conteudo principal.
- Foco visivel em navegacao por teclado.
- Cards navegaveis por `Tab`.
- Mapas com legenda e resumo textual por nivel (incluindo estados/cidades por classificacao).

## 8. Estrutura final de repositorio

```text
backend/         -> API Spring Boot (DDD + Clean)
frontend/        -> SPA React
data-pipeline/   -> ETL Python
docs/            -> documentacao funcional e arquitetural
docker-compose.yml -> infraestrutura local (PostgreSQL)
```

## 9. Como explicar em banca (resumo)
- Problema: vigilancia epidemiologica e resposta operacional lenta/fragmentada.
- Solucao: pipeline + API de regras + dashboard acessivel.
- Diferencial tecnico: dominio modelado, separacao por camadas, testes robustos, foco em acessibilidade e operacao real.
