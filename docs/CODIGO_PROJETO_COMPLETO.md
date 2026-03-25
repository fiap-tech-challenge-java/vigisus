# Codigo do Projeto Completo (Guia de estudo para faculdade)

Este documento e o ponto de entrada para estudar o projeto inteiro.

## 1. Ordem recomendada de leitura
1. `docs/ARQUITETURA_FINAL_PROJETO.md`
2. `docs/CODIGO_DATA_PIPELINE.md`
3. `docs/CODIGO_BACKEND.md`
4. `docs/CODIGO_FRONTEND.md`

## 2. Como explicar "como foi feito cada codigo"

### 2.1 Data Pipeline (Python)
- Cada script `ingest_*` resolve um problema de dados especifico.
- Clientes em `clients/*` isolam APIs externas e FTP.
- `run_all.py` define fluxo padrao reproduzivel.

### 2.2 Backend (Java/Spring)
- Camadas separadas por responsabilidade:
  - `controller` (entrada HTTP),
  - `application` (casos de uso),
  - `domain` (regras),
  - `infrastructure` (adapters),
  - `repository/model` (persistencia).

### 2.3 Frontend (React)
- `App.jsx` controla rotas e composicao.
- Paginas agregam componentes por contexto de tela.
- Componentes representam blocos de dominio (nao apenas visuais).
- Acessibilidade e UX sao tratadas como requisito de arquitetura.

## 3. Argumento tecnico para banca
- O projeto nao e apenas CRUD:
  - tem ETL com dados reais,
  - regras de negocio explicitas de risco/operacao,
  - arquitetura em camadas com baixo acoplamento,
  - testes automatizados relevantes,
  - foco em acessibilidade de interface.

## 4. Evidencias praticas
- Cobertura backend em JaCoCo (`backend/target/site/jacoco/*`).
- Build e testes frontend (`frontend`).
- Scripts de validacao e ingestao no pipeline (`data-pipeline`).
