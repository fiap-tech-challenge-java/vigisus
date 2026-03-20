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
        INSERT INTO municipios (id, co_ibge, no_municipio, sg_uf, nu_latitude, nu_longitude)
        VALUES (:id, :co_ibge, :no_municipio, :sg_uf, :nu_latitude, :nu_longitude)
        ON CONFLICT (id) DO UPDATE
            SET no_municipio  = EXCLUDED.no_municipio,
                sg_uf         = EXCLUDED.sg_uf,
                nu_latitude   = EXCLUDED.nu_latitude,
                nu_longitude  = EXCLUDED.nu_longitude
        """
    )

    records = []
    for m in municipios:
        centroide = m.get("centroide") or {}
        
        # Extrai UF com segurança: tenta mesorregiao.UF, depois microrregiao.UF, depois extrai do código IBGE
        sg_uf = None
        
        # Tentativa 1: mesorregiao.UF.sigla
        if (m.get("microrregiao") and 
            m["microrregiao"].get("mesorregiao") and 
            m["microrregiao"]["mesorregiao"].get("UF")):
            sg_uf = m["microrregiao"]["mesorregiao"]["UF"].get("sigla")
        
        # Tentativa 2: microrregiao.UF.sigla
        if not sg_uf and m.get("microrregiao") and m["microrregiao"].get("UF"):
            sg_uf = m["microrregiao"]["UF"].get("sigla")
        
        # Tentativa 3: extrai dos primeiros 2 dígitos do código IBGE (mapeamento de UFs)
        if not sg_uf:
            co_ibge = str(m.get("id", ""))
            if len(co_ibge) >= 2:
                # Mapa de código IBGE para sigla UF (primeiros 2 dígitos)
                uf_map = {
                    "11": "RO", "12": "AC", "13": "AM", "14": "RR", "15": "PA", "16": "AP", "17": "TO",
                    "21": "MA", "22": "PI", "23": "CE", "24": "RN", "25": "PB", "26": "PE", "27": "AL", "28": "SE", "29": "BA",
                    "31": "MG", "32": "ES", "33": "RJ", "35": "SP",
                    "41": "PR", "42": "SC", "43": "RS",
                    "50": "MS", "51": "MT", "52": "GO", "53": "DF"
                }
                sg_uf = uf_map.get(co_ibge[:2])
        
        # Se ainda não fez match, loga warning e pula este município
        if not sg_uf:
            logger.warning("Não foi possível extrair UF para município %s (id=%s)", m.get("nome"), m.get("id"))
            continue
        
        records.append(
            {
                "id": int(m["id"]),
                "co_ibge": str(m["id"]),
                "no_municipio": m["nome"],
                "sg_uf": sg_uf,
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
