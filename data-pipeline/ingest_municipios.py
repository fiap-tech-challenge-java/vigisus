"""
ingest_municipios.py
Carrega municipios da API do IBGE e faz upsert na tabela municipios.
"""

from __future__ import annotations

import logging

from sqlalchemy import create_engine, text

from clients.requests_config import session
from config import DB_URL, IBGE_BASE_URL

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

# Coordenadas conhecidas para principais municipios.
COORDENADAS_CONHECIDAS = {
    "3131307": (-21.2453, -44.9994),
    "3106200": (-19.9191, -43.9386),
    "3170206": (-18.9186, -48.2772),
    "3136702": (-21.7642, -43.3503),
    "3143302": (-16.7282, -43.8617),
    "3152501": (-22.2289, -45.9373),
    "3167202": (-19.7483, -47.9317),
    "3122306": (-19.9319, -44.0536),
    "3147105": (-20.7189, -46.6100),
    "3136207": (-18.8514, -41.9494),
    "3155504": (-19.7706, -44.0858),
    "3163706": (-21.5512, -45.4302),
    "3131703": (-19.4683, -42.5364),
    "3169307": (-19.7338, -42.8619),
    "3118601": (-19.5186, -42.6347),
    "3550308": (-23.5505, -46.6333),
    "3509502": (-22.9056, -47.0608),
    "3548708": (-21.1775, -47.8103),
    "3529401": (-23.5329, -46.7920),
    "3518800": (-23.4543, -46.5333),
    "3543402": (-22.7253, -47.6492),
    "3525904": (-22.2139, -49.9458),
    "3557105": (-23.5015, -47.4526),
    "3547809": (-23.9618, -46.3322),
    "3504009": (-22.3000, -49.0697),
}

UF_POR_PREFIXO_IBGE = {
    "11": "RO",
    "12": "AC",
    "13": "AM",
    "14": "RR",
    "15": "PA",
    "16": "AP",
    "17": "TO",
    "21": "MA",
    "22": "PI",
    "23": "CE",
    "24": "RN",
    "25": "PB",
    "26": "PE",
    "27": "AL",
    "28": "SE",
    "29": "BA",
    "31": "MG",
    "32": "ES",
    "33": "RJ",
    "35": "SP",
    "41": "PR",
    "42": "SC",
    "43": "RS",
    "50": "MS",
    "51": "MT",
    "52": "GO",
    "53": "DF",
}


def _fallback_sg_uf(co_ibge: str) -> str:
    return UF_POR_PREFIXO_IBGE.get(co_ibge[:2], "XX")


def _extract_sg_uf(payload: dict) -> str | None:
    try:
        return (
            payload.get("microrregiao", {})
            .get("mesorregiao", {})
            .get("UF", {})
            .get("sigla")
        )
    except Exception:  # noqa: BLE001
        return None


def fetch_municipios() -> list[dict]:
    url = f"{IBGE_BASE_URL}/v1/localidades/municipios"
    logger.info("Buscando municipios do IBGE: %s", url)
    response = session.get(url, timeout=60)
    response.raise_for_status()
    data = response.json()
    if not isinstance(data, list):
        raise RuntimeError("Resposta inesperada da API do IBGE para municipios")
    return data


def upsert_municipios(municipios: list[dict]) -> int:
    engine = create_engine(DB_URL)
    sql = text(
        """
        INSERT INTO municipios (id, co_ibge, no_municipio, sg_uf, nu_latitude, nu_longitude)
        VALUES (:id, :co_ibge, :no_municipio, :sg_uf, :nu_latitude, :nu_longitude)
        ON CONFLICT (id) DO UPDATE
            SET no_municipio = EXCLUDED.no_municipio,
                sg_uf = EXCLUDED.sg_uf,
                nu_latitude = EXCLUDED.nu_latitude,
                nu_longitude = EXCLUDED.nu_longitude
        """
    )

    records: list[dict] = []
    for m in municipios:
        raw_id = m.get("id")
        if raw_id is None:
            logger.warning("Payload inesperado do IBGE (sem id): %s", m)
            continue

        co_ibge = str(raw_id)
        lat, lon = COORDENADAS_CONHECIDAS.get(co_ibge, (None, None))
        sg_uf = _extract_sg_uf(m) or _fallback_sg_uf(co_ibge)

        records.append(
            {
                "id": co_ibge,
                "co_ibge": co_ibge,
                "no_municipio": m.get("nome"),
                "sg_uf": sg_uf,
                "nu_latitude": lat,
                "nu_longitude": lon,
            }
        )

    if not records:
        logger.warning("Nenhum municipio retornado para upsert.")
        return 0

    with engine.begin() as conn:
        conn.execute(sql, records)
    return len(records)


def run() -> None:
    municipios = fetch_municipios()
    total = upsert_municipios(municipios)
    logger.info("Importados %d municipios", total)


if __name__ == "__main__":
    run()
