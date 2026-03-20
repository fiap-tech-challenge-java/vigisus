"""
clients/openmeteo_client.py
Cliente para a API Open-Meteo (dados climáticos gratuitos, sem autenticação).
Fornece clima atual, previsão de 16 dias e histórico climático.
"""

import logging

from .requests_config import session

logger = logging.getLogger(__name__)

_BASE_URL = "https://api.open-meteo.com/v1"
_TIMEOUT = 20


def _get(path: str, params: dict) -> dict | None:
    """Executa GET e retorna JSON ou None com log de erro."""
    url = f"{_BASE_URL}{path}"
    try:
        response = session.get(url, params=params, timeout=_TIMEOUT)
        response.raise_for_status()
        return response.json()
    except requests.RequestException as exc:
        logger.error("Open-Meteo indisponível (%s): %s", url, exc)
        return None


def get_clima_atual(lat: float, lon: float) -> dict | None:
    """
    Retorna o clima atual para as coordenadas informadas.

    :param lat: Latitude.
    :param lon: Longitude.
    :return: { temperatura, umidade, precipitacao, vento_kmh, timestamp } ou None.
    """
    params = {
        "latitude": lat,
        "longitude": lon,
        "current": "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m",
        "timezone": "America/Sao_Paulo",
    }
    data = _get("/forecast", params)
    if data is None:
        return None
    try:
        current = data.get("current", {})
        return {
            "temperatura": current.get("temperature_2m"),
            "umidade": current.get("relative_humidity_2m"),
            "precipitacao": current.get("precipitation"),
            "vento_kmh": current.get("wind_speed_10m"),
            "timestamp": current.get("time"),
        }
    except Exception as exc:
        logger.error("Erro ao parsear clima atual: %s", exc)
        return None


def get_previsao_16_dias(lat: float, lon: float) -> list[dict]:
    """
    Retorna a previsão diária para os próximos 16 dias.

    :param lat: Latitude.
    :param lon: Longitude.
    :return: Lista de { data, temp_max, temp_min, chuva_mm, prob_chuva_pct }.
    """
    params = {
        "latitude": lat,
        "longitude": lon,
        "daily": "temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max",
        "forecast_days": 16,
        "timezone": "America/Sao_Paulo",
    }
    data = _get("/forecast", params)
    if data is None:
        return []
    try:
        daily = data.get("daily", {})
        dates = daily.get("time", [])
        temp_max = daily.get("temperature_2m_max", [])
        temp_min = daily.get("temperature_2m_min", [])
        chuva = daily.get("precipitation_sum", [])
        prob_chuva = daily.get("precipitation_probability_max", [])

        result = []
        for i, date in enumerate(dates):
            result.append({
                "data": date,
                "temp_max": temp_max[i] if i < len(temp_max) else None,
                "temp_min": temp_min[i] if i < len(temp_min) else None,
                "chuva_mm": chuva[i] if i < len(chuva) else None,
                "prob_chuva_pct": prob_chuva[i] if i < len(prob_chuva) else None,
            })
        return result
    except Exception as exc:
        logger.error("Erro ao parsear previsão 16 dias: %s", exc)
        return []


def get_historico_climatico(
    lat: float,
    lon: float,
    data_inicio: str,
    data_fim: str,
) -> list[dict]:
    """
    Retorna o histórico climático diário entre duas datas.

    Útil para correlacionar clima passado com surtos epidemiológicos.

    :param lat: Latitude.
    :param lon: Longitude.
    :param data_inicio: Data inicial no formato YYYY-MM-DD.
    :param data_fim: Data final no formato YYYY-MM-DD.
    :return: Lista de { data, temp_max, chuva_mm }.
    """
    params = {
        "latitude": lat,
        "longitude": lon,
        "start_date": data_inicio,
        "end_date": data_fim,
        "daily": "temperature_2m_max,precipitation_sum",
        "timezone": "America/Sao_Paulo",
    }
    # O endpoint de arquivo histórico é /archive, não /forecast
    url = "https://archive-api.open-meteo.com/v1/archive"
    try:
        response = session.get(url, params=params, timeout=_TIMEOUT)
        response.raise_for_status()
        data = response.json()
    except requests.RequestException as exc:
        logger.error("Open-Meteo histórico indisponível: %s", exc)
        return []

    try:
        daily = data.get("daily", {})
        dates = daily.get("time", [])
        temp_max = daily.get("temperature_2m_max", [])
        chuva = daily.get("precipitation_sum", [])

        result = []
        for i, date in enumerate(dates):
            result.append({
                "data": date,
                "temp_max": temp_max[i] if i < len(temp_max) else None,
                "chuva_mm": chuva[i] if i < len(chuva) else None,
            })
        return result
    except Exception as exc:
        logger.error("Erro ao parsear histórico climático: %s", exc)
        return []
