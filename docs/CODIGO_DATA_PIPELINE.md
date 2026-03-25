# Data Pipeline - Documentacao de Codigo

## 1. Objetivo do modulo
O modulo `data-pipeline/` e responsavel por ingestao, conversao, enriquecimento e carga de dados para o banco PostgreSQL usado pelo backend.

Principais fontes:
- IBGE (municipios e populacao),
- DATASUS/CNES/SINAN (infraestrutura assistencial e casos),
- OpenDataSUS e Open-Meteo (apoio analitico e validacoes).

## 2. Arquitetura interna

```text
Scripts de orquestracao
   -> scripts de ingestao por dominio
      -> clientes de integracao externa
         -> banco PostgreSQL
```

- `run_all.py` orquestra as etapas principais.
- `ingest_*` implementam regras de transformacao por fonte.
- `clients/*` encapsulam acesso a APIs/FTP e conversoes.

## 3. Mapa de codigo (arquivo por arquivo)

### 3.1 Scripts principais (`data-pipeline/`)

| Arquivo | Responsabilidade | Entrada | Saida |
|---|---|---|---|
| `run_all.py` | Executa pipeline completo em ordem | configuracao e modulos `ingest_*` | execucao encadeada com logs de progresso |
| `config.py` | Centraliza parametros globais | variaveis de ambiente | constantes de conexao e endpoints |
| `ingest_municipios.py` | Busca municipios no IBGE e faz upsert | API IBGE | tabela `municipios` |
| `ingest_populacao.py` | Preenche populacao por municipio | API SIDRA/IBGE | campo `municipios.populacao` |
| `ingest_planilhas_saude.py` | Processa ST/LT/SR/DENG (CSV/Excel) | pasta `csv_input` e fallback `data/csv` | tabelas `estabelecimentos`, `leitos`, `servicos_especializados`, `casos_dengue` |
| `ingest_cnes.py` | Ingestao CNES a partir de DBC via FTP | FTP DATASUS | tabelas de rede assistencial |
| `ingest_sinan_dengue.py` | Ingestao SINAN dengue por ano/semana | FTP DATASUS + DBC | `casos_dengue` agregado |
| `cnes_ftp_download.py` | Download manual de arquivos CNES via FTP | filtros de UF/competencia | arquivos `.dbc` locais |
| `cnes_download_converter.py` | Download CNES (servico web) e conversao CSV->JSON | endpoint CNES export | arquivo `.json` |
| `sus_completo.py` | utilitario de execucao ampla da base SUS | fontes configuradas | carga consolidada |
| `seed_mock.py` | gera massa de dados de demonstracao | sem dependencia externa | dados fake no banco |
| `validar_apis.py` | valida disponibilidade de fontes externas | endpoints publicos | relatorio de status |
| `_check_counts.py` | checagem rapida de contagens no banco | base carregada | diagnostico de consistencia |

### 3.2 Clientes (`data-pipeline/clients/`)

| Arquivo | Responsabilidade |
|---|---|
| `requests_config.py` | sessao HTTP padrao com retry, pool e SSL configuravel |
| `ibge_client.py` | cliente IBGE/SIDRA para municipio, populacao e coordenadas |
| `openmeteo_client.py` | cliente Open-Meteo (atual, previsao, historico) |
| `opendatasus_client.py` | busca datasets CKAN e download de CSV |
| `datasus_ftp_client.py` | listagem e download de arquivos FTP DATASUS |
| `dbc_converter.py` | conversao de DBC para DataFrame/CSV com fallback de bibliotecas |
| `__init__.py` | marca pacote python |

## 4. Padroes de implementacao adotados
- ETL incremental/total conforme tabela e fonte.
- Idempotencia operacional (upsert e/ou recarga controlada).
- Parsing resiliente (aliases de coluna, fallback de codificacao).
- Tratamento defensivo de erros de rede e parse.
- Logging de progresso para arquivos grandes.

## 5. Melhorias aplicadas nesta revisao

### 5.1 Qualidade e seguranca de cliente HTTP
- `clients/requests_config.py` refatorado para:
  - retry automatico em falhas transientes,
  - pool de conexoes,
  - SSL configuravel por `REQUESTS_VERIFY_SSL` (sem desabilitar por padrao).

### 5.2 Correcao de bug de runtime
- `clients/openmeteo_client.py` reescrito e corrigido:
  - import de `requests`,
  - parsing de listas mais robusto (`zip_longest`),
  - centralizacao de constantes e tratamento de erro.

### 5.3 Otimizacao de FTP DATASUS
- `clients/datasus_ftp_client.py` reescrito:
  - cache de listagem por pasta,
  - lookup case-insensitive sem repeticao de loops custosos.

### 5.4 Otimizacao de download CNES
- `cnes_download_converter.py` reescrito:
  - download em streaming (menor uso de memoria),
  - reaproveitamento de sessao HTTP padronizada.

### 5.5 Performance de escrita no banco
- `ingest_populacao.py` reescrito:
  - updates em lote (`POP_UPDATE_BATCH_SIZE`) em vez de transacao por registro,
  - reutilizacao de sessao HTTP compartilhada.

### 5.6 Padronizacao de ingestao de municipios
- `ingest_municipios.py` reescrito:
  - uso da sessao HTTP padrao,
  - extracao de UF desacoplada em funcao dedicada,
  - validacoes de payload mais claras.

## 6. Parametros de ambiente relevantes

| Variavel | Efeito |
|---|---|
| `DB_URL` | conexao com PostgreSQL |
| `DATA_DIR` | diretorio de arquivos intermediarios |
| `SPREADSHEET_DIR` | pasta principal de planilhas |
| `SPREADSHEET_FALLBACK_DIR` | pasta fallback de planilhas |
| `REQUESTS_VERIFY_SSL` | ativa/desativa verificacao SSL |
| `HTTP_RETRY_TOTAL` | numero de retries HTTP |
| `HTTP_RETRY_BACKOFF` | backoff entre retries |
| `POP_REQUEST_SLEEP_SECONDS` | pausa entre requests de populacao |
| `POP_UPDATE_BATCH_SIZE` | tamanho do lote de updates de populacao |

## 7. Como explicar em apresentacao
- Primeiro, mostrar o papel de cada script no fluxo de ingestao.
- Depois, destacar resiliencia (retry/fallback) e performance (batch/chunks/cache).
- Por fim, mostrar como o pipeline abastece diretamente os endpoints do backend.
