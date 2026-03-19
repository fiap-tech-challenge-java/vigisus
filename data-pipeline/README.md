# VígiSUS — Data Pipeline

Pipeline de ingestão de dados públicos do DATASUS, IBGE e CNES para o banco PostgreSQL do VígiSUS.

## Instalação

```bash
pip install pysus pandas requests psycopg2-binary sqlalchemy tabulate
```

## Antes de tudo — valide as fontes

```bash
python validar_apis.py
```

> Deve retornar **7/7 OK**

## Execução completa (primeira vez)

```bash
python run_all.py
```

Tempo estimado: **15-30 minutos**
(a maior parte é download do FTP)

## Execução individual

```bash
python ingest_municipios.py    # → ~5 min
python ingest_populacao.py     # → ~10 min
python ingest_cnes.py          # → ~5 min
python ingest_sinan_dengue.py  # → ~10 min
```

## Dados mockados (sem internet / demo rápida)

```bash
python seed_mock.py
```

Insere Lavras + 3 hospitais + 2 anos de dengue.
Suficiente para demonstrar todos os endpoints.

## Agendamento (produção)

Adicionar ao cron para rodar toda semana:

```cron
0 2 * * 0 cd /app/data-pipeline && python run_all.py
```

## Estrutura dos arquivos baixados

```
downloads/
  DENGBR23.dbc   → dengue Brasil 2023
  DENGBR24.dbc   → dengue Brasil 2024
  DENGBR25.dbc   → dengue Brasil 2025 (prelim)
  STMG2502.dbc   → estabelecimentos MG fev/2025
  LTMG2502.dbc   → leitos MG fev/2025
  SRMG2502.dbc   → serviços MG fev/2025
```
