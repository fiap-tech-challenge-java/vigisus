"""
ingest_populacao.py
Para cada município importado, busca a população 2023 na API do IBGE e
atualiza o campo populacao na tabela municipios.
Processa em batches de 50 municípios para não sobrecarregar a API.
"""

import logging
import time

import requests
from sqlalchemy import create_engine, text

from config import DB_URL, IBGE_BASE_URL

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

BATCH_SIZE = 50
IBGE_AGREGADO = "6579"
IBGE_VARIAVEL = "9324"
IBGE_PERIODO = "2023"


def fetch_co_ibge_list(engine) -> list:
    """Retorna a lista de co_ibge já importados."""
    with engine.connect() as conn:
        result = conn.execute(text("SELECT co_ibge FROM municipios ORDER BY co_ibge"))
        return [row[0] for row in result]


def fetch_populacao_batch(co_ibge_list: list) -> dict:
    """
    Busca a população 2023 de um batch de municípios.
    Retorna dict {co_ibge: populacao}.
    """
    localidades = "|".join(co_ibge_list)
    url = (
        f"{IBGE_BASE_URL}/v3/agregados/{IBGE_AGREGADO}"
        f"/periodos/{IBGE_PERIODO}/variaveis/{IBGE_VARIAVEL}"
        f"?localidades=N6[{localidades}]"
    )
    response = requests.get(url, timeout=60)
    response.raise_for_status()
    data = response.json()

    resultado = {}
    for variavel in data:
        for item in variavel.get("resultados", []):
            for localidade in item.get("localidades", []):
                co = str(localidade["id"])
                valor = item["series"][0]["serie"].get(IBGE_PERIODO)
                if valor and valor != "-":
                    try:
                        resultado[co] = int(valor)
                    except ValueError:
                        pass
    return resultado


def update_populacao(engine, populacao_map: dict) -> int:
    """Atualiza o campo populacao para cada município no mapa."""
    sql = text(
        "UPDATE municipios SET populacao = :populacao WHERE co_ibge = :co_ibge"
    )
    updated = 0
    with engine.begin() as conn:
        for co_ibge, populacao in populacao_map.items():
            conn.execute(sql, {"populacao": populacao, "co_ibge": co_ibge})
            updated += 1
    return updated


def run() -> None:
    engine = create_engine(DB_URL)
    co_ibge_list = fetch_co_ibge_list(engine)
    logger.info("Total de municípios para atualizar: %d", len(co_ibge_list))

    total_updated = 0
    for i in range(0, len(co_ibge_list), BATCH_SIZE):
        batch = co_ibge_list[i : i + BATCH_SIZE]
        logger.info(
            "Processando batch %d/%d (%d municípios)",
            i // BATCH_SIZE + 1,
            (len(co_ibge_list) + BATCH_SIZE - 1) // BATCH_SIZE,
            len(batch),
        )
        try:
            populacao_map = fetch_populacao_batch(batch)
            total_updated += update_populacao(engine, populacao_map)
        except Exception as exc:
            logger.warning("Erro no batch %d: %s", i // BATCH_SIZE + 1, exc)
        time.sleep(0.5)

    logger.info("Atualizada população de %d municípios", total_updated)


if __name__ == "__main__":
    run()
