"""
config.py
Centraliza todas as configurações do pipeline de ingestão de dados do VígiSUS.
"""

import os

# Banco de dados
# Sobrescreva com a variável de ambiente DB_URL em produção.
DB_URL = os.environ.get(
    "DB_URL",
    "postgresql://vigisus:vigisus123@localhost:5432/vigisus",
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
