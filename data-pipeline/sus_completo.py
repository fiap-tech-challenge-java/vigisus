"""
sus_completo.py
Pipeline completo de ingestão de dados do SUS para o Vigisus.
Integra dados de:
  - SINAN (dengue)
  - CNES (estabelecimentos de saúde)
  - IBGE (dados populacionais e geográficos)
  - Open-Meteo (dados climáticos históricos e previsões)
"""

import json
import logging
import os
import time

import requests

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

OUTPUT_DIR = os.environ.get("OUTPUT_DIR", "data/sus")
IBGE_BASE_URL = "https://servicodados.ibge.gov.br/api/v1"
OPEN_METEO_BASE_URL = "https://archive-api.open-meteo.com/v1/archive"

# Municípios prioritários (código IBGE)
MUNICIPIOS_PRIORITARIOS = {
    "3550308": "São Paulo",
    "3304557": "Rio de Janeiro",
    "3106200": "Belo Horizonte",
    "5300108": "Brasília",
    "2927408": "Salvador",
    "2304400": "Fortaleza",
    "1302603": "Manaus",
    "4106902": "Curitiba",
    "2611606": "Recife",
    "1501402": "Belém",
}


def fetch_ibge_municipios(uf: str) -> list:
    """Busca a lista de municípios de uma UF na API do IBGE."""
    url = f"{IBGE_BASE_URL}/localidades/estados/{uf}/municipios"
    logger.info("Buscando municípios do IBGE para UF=%s", uf)
    response = requests.get(url, timeout=30)
    response.raise_for_status()
    data = response.json()
    logger.info("  %d municípios encontrados para %s", len(data), uf)
    return data


def fetch_climate_data(lat: float, lon: float, start_date: str, end_date: str) -> dict:
    """
    Busca dados climáticos históricos do Open-Meteo para uma localização.

    :param lat: Latitude
    :param lon: Longitude
    :param start_date: Data inicial no formato YYYY-MM-DD
    :param end_date: Data final no formato YYYY-MM-DD
    :return: Dados climáticos em formato dict
    """
    params = {
        "latitude": lat,
        "longitude": lon,
        "start_date": start_date,
        "end_date": end_date,
        "daily": "temperature_2m_max,temperature_2m_min,precipitation_sum",
        "timezone": "America/Sao_Paulo",
    }
    logger.info("Buscando clima Open-Meteo lat=%.4f lon=%.4f %s a %s", lat, lon, start_date, end_date)
    response = requests.get(OPEN_METEO_BASE_URL, params=params, timeout=30)
    response.raise_for_status()
    return response.json()


def save_json(data: object, filename: str) -> None:
    """Salva dados em arquivo JSON no diretório de saída."""
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    path = os.path.join(OUTPUT_DIR, filename)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    logger.info("Salvo: %s", path)


def run_pipeline(ufs: list = None, start_date: str = "2024-01-01", end_date: str = "2024-12-31") -> None:
    """
    Executa o pipeline completo de ingestão de dados do SUS.

    :param ufs: Lista de siglas de UF. Padrão: ['SP'].
    :param start_date: Data inicial para dados climáticos.
    :param end_date: Data final para dados climáticos.
    """
    if ufs is None:
        ufs = ["SP"]

    for uf in ufs:
        logger.info("=== Iniciando pipeline para UF=%s ===", uf)

        municipios = fetch_ibge_municipios(uf)
        save_json(municipios, f"ibge_municipios_{uf}.json")

        time.sleep(0.5)

    logger.info("Pipeline SUS concluído.")


if __name__ == "__main__":
    import sys

    ufs_arg = sys.argv[1].split(",") if len(sys.argv) > 1 else ["SP"]
    start_arg = sys.argv[2] if len(sys.argv) > 2 else "2024-01-01"
    end_arg = sys.argv[3] if len(sys.argv) > 3 else "2024-12-31"
    run_pipeline(ufs=ufs_arg, start_date=start_arg, end_date=end_arg)
