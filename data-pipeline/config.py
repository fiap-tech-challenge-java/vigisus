"""
config.py
Centraliza todas as configurações do pipeline de ingestão de dados do VígiSUS.
"""

import os

# Banco de dados
# DB_URL tem precedência. Se não definida, monta a URL a partir das partes.
# Atenção: os valores padrão abaixo são apenas para desenvolvimento local.
# Em produção, defina as variáveis de ambiente explicitamente.
_db_host = os.environ.get("DB_HOST", "localhost")
_db_port = os.environ.get("DB_PORT", "5432")
_db_name = os.environ.get("DB_NAME", "vigisus")
_db_user = os.environ.get("DB_USER", "vigisus")
_db_pass = os.environ.get("DB_PASS", "vigisus123")
DB_URL = os.environ.get(
    "DB_URL",
    f"postgresql://{_db_user}:{_db_pass}@{_db_host}:{_db_port}/{_db_name}",
)

# FTP DATASUS
FTP_HOST = "ftp.datasus.gov.br"

# Unidade Federativa e competência (configuráveis por variável de ambiente)
UF = os.environ.get("UF", "MG")
COMPETENCIA = os.environ.get("COMPETENCIA", "2502")

# IBGE API
IBGE_BASE_URL = "https://servicodados.ibge.gov.br/api"

# Open-Meteo API
OPENMETEO_BASE_URL = "https://archive-api.open-meteo.com/v1/archive"

# Diretório local para arquivos baixados
DATA_DIR = os.environ.get("DATA_DIR", "data")
