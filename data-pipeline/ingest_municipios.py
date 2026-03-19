"""
ingest_municipios.py
Baixa todos os municípios do Brasil via API do IBGE e faz upsert na tabela municipios.
"""

import logging

import requests
from sqlalchemy import create_engine, text

from config import DB_URL, IBGE_BASE_URL

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


def fetch_municipios() -> list:
    """Busca todos os municípios do Brasil na API do IBGE."""
    url = f"{IBGE_BASE_URL}/v1/localidades/municipios"
    logger.info("Buscando municípios do IBGE: %s", url)
    response = requests.get(url, timeout=60)
    response.raise_for_status()
    return response.json()


def upsert_municipios(municipios: list) -> int:
    """Faz upsert dos municípios na tabela municipios do banco."""
    engine = create_engine(DB_URL)
    sql = text(
        """
        INSERT INTO municipios (co_ibge, no_municipio, sg_uf, nu_latitude, nu_longitude)
        VALUES (:co_ibge, :no_municipio, :sg_uf, :nu_latitude, :nu_longitude)
        ON CONFLICT (co_ibge) DO UPDATE
            SET no_municipio  = EXCLUDED.no_municipio,
                sg_uf         = EXCLUDED.sg_uf,
                nu_latitude   = EXCLUDED.nu_latitude,
                nu_longitude  = EXCLUDED.nu_longitude
        """
    )

    records = []
    for m in municipios:
        centroide = m.get("centroide") or {}
        records.append(
            {
                "co_ibge": str(m["id"]),
                "no_municipio": m["nome"],
                "sg_uf": m["microrregiao"]["mesorregiao"]["UF"]["sigla"],
                "nu_latitude": centroide.get("latitude"),
                "nu_longitude": centroide.get("longitude"),
            }
        )

    with engine.begin() as conn:
        conn.execute(sql, records)

    return len(records)


def run() -> None:
    municipios = fetch_municipios()
    total = upsert_municipios(municipios)
    logger.info("Importados %d municípios", total)


if __name__ == "__main__":
    run()
