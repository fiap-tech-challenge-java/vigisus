"""
ingest_populacao.py
Atualiza populacao dos municipios usando API do IBGE.
"""

from __future__ import annotations

import logging
import os
import time

from urllib.parse import urlencode

from sqlalchemy import create_engine, text

from clients.requests_config import session
from config import DB_URL, IBGE_BASE_URL

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

IBGE_AGREGADO = "6579"
IBGE_VARIAVEL = "9324"
IBGE_PERIODO = os.environ.get("POP_IBGE_PERIODO", "2023")

REQUEST_SLEEP_SECONDS = float(os.environ.get("POP_REQUEST_SLEEP_SECONDS", "0.2"))
PROGRESS_EVERY = int(os.environ.get("POP_PROGRESS_EVERY", "25"))
HEARTBEAT_SECONDS = int(os.environ.get("POP_HEARTBEAT_SECONDS", "15"))
UPDATE_BATCH_SIZE = max(1, int(os.environ.get("POP_UPDATE_BATCH_SIZE", "100")))

# Tenta reduzir drasticamente o tempo de execução usando requisições em lote.
# A API de agregados do IBGE aceita múltiplas localidades no parâmetro "localidades".
# Ex.: localidades=N6[3550308,3304557]
IBGE_BATCH_SIZE = max(1, int(os.environ.get("POP_IBGE_BATCH_SIZE", "100")))

# Cache simples do melhor período disponível para evitar 5500 chamadas em anos vazios.
_CACHED_IBGE_PERIODO_OK: str | None = None


def _discover_periodo_disponivel(preferido: str) -> str:
    """Descobre um período (ano) com dados no agregado de estimativa populacional.

    Em alguns anos (ex.: 2022/2023) o IBGE pode retornar lista vazia para esse agregado.
    Esse método testa o ano preferido e recua até encontrar dados.
    """

    global _CACHED_IBGE_PERIODO_OK  # noqa: PLW0603
    if _CACHED_IBGE_PERIODO_OK:
        return _CACHED_IBGE_PERIODO_OK

    try:
        ano_pref = int(preferido)
    except Exception:  # noqa: BLE001
        ano_pref = 2023

    # Município grande e estável para probe
    probe = "3550308"  # São Paulo

    for ano in range(ano_pref, max(2000, ano_pref - 10), -1):
        url = (
            f"{IBGE_BASE_URL}/v3/agregados/{IBGE_AGREGADO}"
            f"/periodos/{ano}/variaveis/{IBGE_VARIAVEL}?localidades=N6[{probe}]"
        )
        try:
            r = session.get(url, timeout=20)
            r.raise_for_status()
            data = r.json()
            if isinstance(data, list) and len(data) > 0:
                _CACHED_IBGE_PERIODO_OK = str(ano)
                logger.info("IBGE: usando periodo %s (periodo %s estava vazio)", _CACHED_IBGE_PERIODO_OK, preferido)
                return _CACHED_IBGE_PERIODO_OK
        except Exception:  # noqa: BLE001
            continue

    _CACHED_IBGE_PERIODO_OK = str(ano_pref)
    logger.warning("IBGE: nao foi possivel detectar periodo com dados; usando %s", _CACHED_IBGE_PERIODO_OK)
    return _CACHED_IBGE_PERIODO_OK

POPULACAO_FALLBACK_UF = {
    "AC": 15000,
    "AL": 30000,
    "AP": 18000,
    "AM": 40000,
    "BA": 35000,
    "CE": 35000,
    "DF": 200000,
    "ES": 40000,
    "GO": 30000,
    "MA": 25000,
    "MT": 20000,
    "MS": 25000,
    "MG": 35000,
    "PA": 30000,
    "PB": 20000,
    "PR": 40000,
    "PE": 35000,
    "PI": 15000,
    "RJ": 80000,
    "RN": 20000,
    "RS": 40000,
    "RO": 20000,
    "RR": 15000,
    "SC": 35000,
    "SP": 60000,
    "SE": 25000,
    "TO": 12000,
}


def fetch_populacao_municipio(co_ibge: str, max_retries: int = 3) -> int | None:
    periodo = _discover_periodo_disponivel(IBGE_PERIODO)
    url = (
        f"{IBGE_BASE_URL}/v3/agregados/{IBGE_AGREGADO}"
        f"/periodos/{periodo}/variaveis/{IBGE_VARIAVEL}"
        f"?localidades=N6[{co_ibge}]"
    )
    for tentativa in range(1, max_retries + 1):
        try:
            response = session.get(url, timeout=10)
            response.raise_for_status()
            data = response.json()
            valor = data[0]["resultados"][0]["series"][0]["serie"][periodo]
            return int(valor)
        except Exception as exc:  # noqa: BLE001
            logger.debug("Tentativa %d/%d falhou para %s: %s", tentativa, max_retries, co_ibge, exc)
            if tentativa < max_retries:
                time.sleep(tentativa * 2)
    return None


def fetch_populacao_lote(codigos_ibge: list[str], max_retries: int = 3) -> dict[str, int]:
    """Busca população para um lote de municípios em uma única chamada.

    A API do IBGE permite múltiplas localidades no parâmetro "localidades".

    Retorna um dict {co_ibge: populacao} apenas para códigos encontrados.
    """

    if not codigos_ibge:
        return {}

    periodo = _discover_periodo_disponivel(IBGE_PERIODO)
    base = (
        f"{IBGE_BASE_URL}/v3/agregados/{IBGE_AGREGADO}"
        f"/periodos/{periodo}/variaveis/{IBGE_VARIAVEL}"
    )
    params = {"localidades": f"N6[{','.join(codigos_ibge)}]"}
    url = f"{base}?{urlencode(params)}"

    for tentativa in range(1, max_retries + 1):
        try:
            response = session.get(url, timeout=30)
            response.raise_for_status()
            data = response.json()
            if not data:
                return {}

            out: dict[str, int] = {}
            # Formato mais comum: data[0]['resultados'][0]['series'] com localidade.id.
            resultados = data[0].get("resultados", []) if isinstance(data[0], dict) else []
            for resultado in resultados:
                for serie in resultado.get("series", []) or []:
                    localidade = serie.get("localidade", {}) or {}
                    co = str(localidade.get("id", ""))
                    valor = (serie.get("serie", {}) or {}).get(str(periodo))
                    if co and valor and valor != "-":
                        try:
                            out[co] = int(valor)
                        except Exception:  # noqa: BLE001
                            continue

            # Fallback: alguns retornos usam 'localidades' dentro de resultados
            if not out:
                for resultado in resultados:
                    for localidade in resultado.get("localidades", []) or []:
                        co = str(localidade.get("id", ""))
                        for serie in (localidade.get("series", []) or []):
                            valor = (serie.get("serie", {}) or {}).get(str(periodo))
                            if co and valor and valor != "-":
                                try:
                                    out[co] = int(valor)
                                except Exception:  # noqa: BLE001
                                    continue

            return out
        except Exception as exc:  # noqa: BLE001
            logger.debug("Tentativa %d/%d falhou para lote (%d codigos): %s", tentativa, max_retries, len(codigos_ibge), exc)
            if tentativa < max_retries:
                time.sleep(tentativa * 2)

    return {}


def _flush_updates(engine, updates: list[dict]) -> None:
    if not updates:
        return
    with engine.begin() as conn:
        conn.execute(
            text("UPDATE municipios SET populacao = :pop WHERE co_ibge = :co"),
            updates,
        )
    updates.clear()


def run() -> None:
    engine = create_engine(DB_URL)

    with engine.connect() as conn:
        result = conn.execute(
            text(
                """
                SELECT co_ibge, sg_uf FROM municipios
                WHERE populacao IS NULL OR populacao = 0
                ORDER BY
                    CASE sg_uf WHEN 'MG' THEN 0 WHEN 'SP' THEN 1 ELSE 2 END,
                    co_ibge
                """
            )
        )
        municipios = result.fetchall()

    total = len(municipios)
    logger.info("Municipios sem populacao: %d", total)
    if total == 0:
        logger.info("Nenhum municipio pendente. Etapa finalizada sem alteracoes.")
        return

    atualizados = 0
    fallbacks = 0
    started_at = time.time()
    last_progress_log_at = 0.0
    pending_updates: list[dict] = []

    # Processa em lotes para reduzir o tempo total.
    # Mantém fallback por UF quando o IBGE não retorna valor.
    for batch_start in range(0, total, IBGE_BATCH_SIZE):
        batch = municipios[batch_start : batch_start + IBGE_BATCH_SIZE]
        codigos = [co for (co, _uf) in batch]
        by_co = fetch_populacao_lote(codigos)

        for local_idx, (co_ibge, sg_uf) in enumerate(batch, 1):
            i = batch_start + local_idx
            pop = by_co.get(str(co_ibge))
            if pop is None:
                pop = POPULACAO_FALLBACK_UF.get(sg_uf, 20000)
                fallbacks += 1
            else:
                atualizados += 1

            pending_updates.append({"pop": pop, "co": co_ibge})
            if len(pending_updates) >= UPDATE_BATCH_SIZE:
                _flush_updates(engine, pending_updates)

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
                eta_min, eta_sec = divmod(eta_seconds, 60)
                eta_hour, eta_min = divmod(eta_min, 60)
                logger.info(
                    "Populacao IBGE: %d/%d (%.1f%%) | ibge=%d fallback=%d | vel=%.2f mun/s | ETA=%02d:%02d:%02d",
                    i,
                    total,
                    (i / total) * 100,
                    atualizados,
                    fallbacks,
                    rate,
                    eta_hour,
                    eta_min,
                    eta_sec,
                )
                last_progress_log_at = now

        # Pequena pausa entre lotes (se configurada) para reduzir chance de rate-limit.
        if REQUEST_SLEEP_SECONDS > 0:
            time.sleep(REQUEST_SLEEP_SECONDS)

    _flush_updates(engine, pending_updates)

    total_elapsed = time.time() - started_at
    logger.info(
        "Concluido: %d via IBGE, %d via fallback em %.1fs",
        atualizados,
        fallbacks,
        total_elapsed,
    )


if __name__ == "__main__":
    run()
