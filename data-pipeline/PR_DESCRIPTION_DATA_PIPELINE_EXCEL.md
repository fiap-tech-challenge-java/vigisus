## Objetivo
Substituir o fluxo mockado de dados de saúde por ingestao real a partir de planilhas CSV/Excel, mantendo o pipeline integrado com dados de municipios e populacao do IBGE.

## Contexto
- Branch base mais atualizada: `main`
- Branch de trabalho: `feature/data-pipeline-excel-ingestion`
- Proposta: acionar API para municipios/populacao e carregar ST/LT/SR/DENG via planilhas.

## O que foi alterado
- Criado `ingest_planilhas_saude.py` para leitura de CSV/Excel e persistencia no PostgreSQL.
- Regras de mapeamento aplicadas:
  - `DENG*` / `DENGBR*` -> `casos_dengue`
  - `ST*` -> `estabelecimentos`
  - `LT*` -> `leitos`
  - `SR*` -> `servicos_especializados`
- Implementada limpeza transacional das tabelas de saude antes da carga para substituir dados mockados.
- Integrado no `run_all.py` para seguir o fluxo:
  1. `ingest_municipios`
  2. `ingest_populacao`
  3. `ingest_planilhas_saude`
- Adicionada pasta dedicada para entrada de planilhas: `data-pipeline/csv_input/`.
- Mantido fallback automatico para `data-pipeline/data/csv/` quando `csv_input/` estiver vazia.
- Adicionada dependencia `openpyxl` em `requirements.txt`.
- README atualizado com instrucoes do novo fluxo.

## Como validar
1. Instalar dependencias:
   - `pip install -r data-pipeline/requirements.txt`
2. Colocar arquivos na pasta `data-pipeline/csv_input/` (ou usar fallback em `data-pipeline/data/csv/`).
3. Executar pipeline completo:
   - `python data-pipeline/run_all.py`
4. Validar no banco:
   - Presenca de dados em `estabelecimentos`, `leitos`, `servicos_especializados` e `casos_dengue`.
   - Verificar que dados mock anteriores foram substituidos.

## Impacto
- Remove dependencia de FTP/DBC para a carga principal de saude.
- Torna o pipeline aderente aos arquivos fornecidos para o projeto.
- Mantem compatibilidade com estrutura atual de banco e com o fluxo existente do `run_all.py`.

## Arquivos alterados
- `data-pipeline/ingest_planilhas_saude.py`
- `data-pipeline/run_all.py`
- `data-pipeline/requirements.txt`
- `data-pipeline/README.md`
- `data-pipeline/csv_input/.gitkeep`

## Observacoes
- Alteracoes locais nao relacionadas e nao incluidas neste PR:
  - `.gitignore`
  - `data-pipeline/ingest_municipios.py`
