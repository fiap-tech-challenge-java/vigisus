# VígiSUS — Data Pipeline

Pipeline de ingestão de dados públicos do IBGE e planilhas de saúde (CSV/Excel)
para o banco PostgreSQL do VígiSUS.

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

Tempo estimado: **5-15 minutos**
(depende do volume de planilhas)

## Pasta de entrada das planilhas

Use a pasta dedicada:

```
csv_input/
```

Se estiver vazia, o pipeline usa automaticamente o fallback em:

```
data/csv/
```

Mapeamento por prefixo do arquivo:

- `ST*` -> estabelecimentos
- `LT*` -> leitos
- `SR*` -> serviços
- `DENG*` ou `DENGBR*` -> casos de dengue

Os dados das tabelas de saúde são substituídos a cada execução
(remove mocks e recarrega pelos arquivos).

## Execução individual

```bash
python ingest_municipios.py    # → ~5 min
python ingest_populacao.py     # → ~10 min
python ingest_planilhas_saude.py  # → ST/LT/SR/DENG a partir de CSV/Excel
```

## Dados mockados (sem internet / demo rápida)

```bash
python seed_mock.py
```

Insere Lavras + 3 hospitais + 2 anos de dengue.
Suficiente para demonstrar todos os endpoints.

Para retornar aos dados reais, execute novamente:

```bash
python run_all.py
```

## Agendamento (produção)

Adicionar ao cron para rodar toda semana:

```cron
0 2 * * 0 cd /app/data-pipeline && python run_all.py
```

## Estrutura sugerida dos arquivos

```
data/
  ...            → arquivos de apoio já existentes no projeto
csv_input/       → pasta principal versionada para planilhas do usuário
data/csv/        → fallback para arquivos já existentes no repositório
```
