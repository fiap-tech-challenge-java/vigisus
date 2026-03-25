"""
ingest_populacao.py
Para cada município importado, busca a população 2023 na API do IBGE e
atualiza o campo populacao na tabela municipios.
Processa 1 município por vez com retry e fallback por UF.
"""

import logging
import os
import time

import requests
from sqlalchemy import create_engine, text

from config import DB_URL, IBGE_BASE_URL

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

BATCH_SIZE = 1
IBGE_AGREGADO = "6579"
IBGE_VARIAVEL = "9324"
IBGE_PERIODO = "2023"
REQUEST_SLEEP_SECONDS = float(os.environ.get("POP_REQUEST_SLEEP_SECONDS", "0.2"))
PROGRESS_EVERY = int(os.environ.get("POP_PROGRESS_EVERY", "25"))
HEARTBEAT_SECONDS = int(os.environ.get("POP_HEARTBEAT_SECONDS", "15"))

POPULACAO_FALLBACK_UF = {
    "AC": 15000, "AL": 30000, "AP": 18000, "AM": 40000,
    "BA": 35000, "CE": 35000, "DF": 200000, "ES": 40000,
    "GO": 30000, "MA": 25000, "MT": 20000, "MS": 25000,
    "MG": 35000, "PA": 30000, "PB": 20000, "PR": 40000,
    "PE": 35000, "PI": 15000, "RJ": 80000, "RN": 20000,
    "RS": 40000, "RO": 20000, "RR": 15000, "SC": 35000,
    "SP": 60000, "SE": 25000, "TO": 12000,
}


def fetch_populacao_municipio(co_ibge: str, max_retries: int = 3) -> int | None:
    """
    Busca a população 2023 de um único município.
    Retorna o valor inteiro ou None após esgotar as tentativas.
    """
    url = (
        f"{IBGE_BASE_URL}/v3/agregados/{IBGE_AGREGADO}"
        f"/periodos/{IBGE_PERIODO}/variaveis/{IBGE_VARIAVEL}"
        f"?localidades=N6[{co_ibge}]"
    )
    for tentativa in range(1, max_retries + 1):
        try:
            response = requests.get(url, timeout=10)
            response.raise_for_status()
            data = response.json()
            valor = data[0]["resultados"][0]["series"][0]["serie"][IBGE_PERIODO]
            return int(valor)
        except Exception as exc:
            logger.debug("Tentativa %d/%d falhou para %s: %s", tentativa, max_retries, co_ibge, exc)
            if tentativa < max_retries:
                time.sleep(tentativa * 2)
    return None


def run() -> None:
    engine = create_engine(DB_URL)

    with engine.connect() as conn:
        result = conn.execute(text("""
            SELECT co_ibge, sg_uf FROM municipios
            WHERE populacao IS NULL OR populacao = 0
            ORDER BY
                CASE sg_uf WHEN 'MG' THEN 0 WHEN 'SP' THEN 1 ELSE 2 END,
                co_ibge
        """))
        municipios = result.fetchall()

    logger.info("Municípios sem população: %d", len(municipios))

    if not municipios:
        logger.info("Nenhum município pendente. Etapa de população finalizada sem alterações.")
        return

    atualizados = 0
    fallbacks = 0
    total = len(municipios)
    started_at = time.time()
    last_progress_log_at = 0.0

    for i, (co_ibge, sg_uf) in enumerate(municipios, 1):

        pop = fetch_populacao_municipio(co_ibge)

        if pop is None:
            pop = POPULACAO_FALLBACK_UF.get(sg_uf, 20000)
            fallbacks += 1
        else:
            atualizados += 1

        with engine.begin() as conn:
            conn.execute(text(
                "UPDATE municipios SET populacao = :pop WHERE co_ibge = :co"
            ), {"pop": pop, "co": co_ibge})

        if REQUEST_SLEEP_SECONDS > 0:
            time.sleep(REQUEST_SLEEP_SECONDS)

        now = time.time()
        should_log = (
            i == 1
            or i % PROGRESS_EVERY == 0
            or (now - last_progress_log_at) >= HEARTBEAT_SECONDS
            or i == total
        )

        if should_log:
            elapsed = max(now - started_at, 0.001)
            rate = i / elapsed
            eta_seconds = int((total - i) / rate) if rate > 0 else 0
            pct = (i / total) * 100
            eta_min, eta_sec = divmod(eta_seconds, 60)
            eta_hour, eta_min = divmod(eta_min, 60)
            logger.info(
                "População IBGE: %d/%d (%.1f%%) | ibge=%d fallback=%d | vel=%.2f mun/s | ETA=%02d:%02d:%02d",
                i,
                total,
                pct,
                atualizados,
                fallbacks,
                rate,
                eta_hour,
                eta_min,
                eta_sec,
            )
            last_progress_log_at = now

    total_elapsed = time.time() - started_at
    logger.info(
        "Concluído: %d via IBGE, %d via fallback em %.1fs",
        atualizados,
        fallbacks,
        total_elapsed,
    )


if __name__ == "__main__":
    run()
