"""
clients/openmeteo_client.py
Cliente para API Open-Meteo (clima atual, previsao e historico).
"""

from __future__ import annotations

import logging
from itertools import zip_longest

import requests

from .requests_config import session

logger = logging.getLogger(__name__)

_BASE_URL = "https://api.open-meteo.com/v1"
_ARCHIVE_URL = "https://archive-api.open-meteo.com/v1/archive"
_TIMEOUT = 20
_TIMEZONE = "America/Sao_Paulo"


def _get(path: str, params: dict) -> dict | None:
    """Executa GET e retorna payload JSON em caso de sucesso."""
    url = f"{_BASE_URL}{path}"
    try:
        response = session.get(url, params=params, timeout=_TIMEOUT)
        response.raise_for_status()
        return response.json()
    except requests.RequestException as exc:
        logger.error("Open-Meteo indisponivel (%s): %s", url, exc)
        return None


def _build_daily_rows(daily: dict, include_temp_min: bool) -> list[dict]:
    dates = daily.get("time", [])
    temp_max = daily.get("temperature_2m_max", [])
    chuva = daily.get("precipitation_sum", [])

    if include_temp_min:
        temp_min = daily.get("temperature_2m_min", [])
        prob_chuva = daily.get("precipitation_probability_max", [])
        rows = []
        for date, tmax, tmin, chuva_mm, prob in zip_longest(
            dates, temp_max, temp_min, chuva, prob_chuva, fillvalue=None
        ):
            if date is None:
                continue
            rows.append(
                {
                    "data": date,
                    "temp_max": tmax,
                    "temp_min": tmin,
                    "chuva_mm": chuva_mm,
                    "prob_chuva_pct": prob,
                }
            )
        return rows

    rows = []
    for date, tmax, chuva_mm in zip_longest(dates, temp_max, chuva, fillvalue=None):
        if date is None:
            continue
        rows.append({"data": date, "temp_max": tmax, "chuva_mm": chuva_mm})
    return rows


def get_clima_atual(lat: float, lon: float) -> dict | None:
    params = {
        "latitude": lat,
        "longitude": lon,
        "current": "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m",
        "timezone": _TIMEZONE,
    }
    data = _get("/forecast", params)
    if data is None:
        return None

    current = data.get("current", {})
    return {
        "temperatura": current.get("temperature_2m"),
        "umidade": current.get("relative_humidity_2m"),
        "precipitacao": current.get("precipitation"),
        "vento_kmh": current.get("wind_speed_10m"),
        "timestamp": current.get("time"),
    }


def get_previsao_16_dias(lat: float, lon: float) -> list[dict]:
    params = {
        "latitude": lat,
        "longitude": lon,
        "daily": "temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max",
        "forecast_days": 16,
        "timezone": _TIMEZONE,
    }
    data = _get("/forecast", params)
    if data is None:
        return []

    try:
        return _build_daily_rows(data.get("daily", {}), include_temp_min=True)
    except Exception as exc:  # noqa: BLE001
        logger.error("Erro ao parsear previsao 16 dias: %s", exc)
        return []


def get_historico_climatico(
    lat: float,
    lon: float,
    data_inicio: str,
    data_fim: str,
) -> list[dict]:
    params = {
        "latitude": lat,
        "longitude": lon,
        "start_date": data_inicio,
        "end_date": data_fim,
        "daily": "temperature_2m_max,precipitation_sum",
        "timezone": _TIMEZONE,
    }

    try:
        response = session.get(_ARCHIVE_URL, params=params, timeout=_TIMEOUT)
        response.raise_for_status()
        payload = response.json()
    except requests.RequestException as exc:
        logger.error("Open-Meteo historico indisponivel: %s", exc)
        return []

    try:
        return _build_daily_rows(payload.get("daily", {}), include_temp_min=False)
    except Exception as exc:  # noqa: BLE001
        logger.error("Erro ao parsear historico climatico: %s", exc)
        return []
