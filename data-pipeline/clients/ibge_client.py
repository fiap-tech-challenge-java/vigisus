"""
clients/ibge_client.py
Cliente para a API REST do IBGE (Instituto Brasileiro de Geografia e Estatística).
Fornece dados de municípios, populações estimadas e coordenadas geográficas.
"""

import logging
import time

import requests

logger = logging.getLogger(__name__)

BASE_URL = "https://servicodados.ibge.gov.br"
_TIMEOUT = 15
_MAX_RETRIES = 3
_BACKOFF = 2


def _get(url: str, params: dict = None) -> requests.Response | None:
    """Executa GET com retry automático (3x, backoff 2s)."""
    for attempt in range(1, _MAX_RETRIES + 1):
        try:
            response = requests.get(url, params=params, timeout=_TIMEOUT)
            response.raise_for_status()
            return response
        except requests.RequestException as exc:
            logger.error("Tentativa %d/%d falhou para %s: %s", attempt, _MAX_RETRIES, url, exc)
            if attempt < _MAX_RETRIES:
                time.sleep(_BACKOFF)
    return None


def get_municipio(co_ibge: str) -> dict | None:
    """
    Retorna dados básicos de um município pelo código IBGE.

    :param co_ibge: Código IBGE do município (7 dígitos).
    :return: { co_ibge, no_municipio, sg_uf } ou None em caso de erro.
    """
    url = f"{BASE_URL}/api/v1/localidades/municipios/{co_ibge}"
    response = _get(url)
    if response is None:
        return None
    try:
        data = response.json()
        return {
            "co_ibge": str(data.get("id", co_ibge)),
            "no_municipio": data.get("nome"),
            "sg_uf": data.get("microrregiao", {})
                        .get("mesorregiao", {})
                        .get("UF", {})
                        .get("sigla"),
        }
    except Exception as exc:
        logger.error("Erro ao parsear municipio %s: %s", co_ibge, exc)
        return None


def get_municipios_por_uf(uf: str) -> list[dict]:
    """
    Retorna todos os municípios de uma UF.

    :param uf: Sigla da UF (ex.: 'SP').
    :return: Lista de { co_ibge, no_municipio, sg_uf }.
    """
    url = f"{BASE_URL}/api/v1/localidades/estados/{uf}/municipios"
    response = _get(url)
    if response is None:
        return []
    try:
        data = response.json()
        result = []
        for item in data:
            result.append({
                "co_ibge": str(item.get("id")),
                "no_municipio": item.get("nome"),
                "sg_uf": item.get("microrregiao", {})
                              .get("mesorregiao", {})
                              .get("UF", {})
                              .get("sigla"),
            })
        return result
    except Exception as exc:
        logger.error("Erro ao parsear municípios da UF %s: %s", uf, exc)
        return []


def get_populacao(co_ibge: str, ano: int = 2023) -> int | None:
    """
    Retorna a população estimada de um município para o ano informado.

    Consulta o agregado SIDRA 6579 (Estimativas de população), variável 9324.

    :param co_ibge: Código IBGE do município (7 dígitos).
    :param ano: Ano da estimativa (padrão 2023).
    :return: População estimada como int ou None em caso de erro.
    """
    url = (
        f"{BASE_URL}/api/v3/agregados/6579/periodos/{ano}/variaveis/9324"
    )
    params = {"localidades": f"N6[{co_ibge}]"}
    response = _get(url, params=params)
    if response is None:
        return None
    try:
        data = response.json()
        valor = (
            data[0]
            .get("resultados", [{}])[0]
            .get("series", [{}])[0]
            .get("serie", {})
            .get(str(ano))
        )
        return int(valor) if valor and valor != "-" else None
    except Exception as exc:
        logger.error("Erro ao obter população de %s (%d): %s", co_ibge, ano, exc)
        return None


def get_coordenadas(co_ibge: str) -> tuple[float | None, float | None]:
    """
    Retorna a latitude e longitude do centróide de um município.

    :param co_ibge: Código IBGE do município (7 dígitos).
    :return: (latitude, longitude) ou (None, None) em caso de erro.
    """
    url = f"{BASE_URL}/api/v1/localidades/municipios/{co_ibge}"
    response = _get(url)
    if response is None:
        return (None, None)
    try:
        data = response.json()
        centroide = data.get("centroide") or data.get("municipio", {}).get("centroide")
        if centroide and "latitude" in centroide and "longitude" in centroide:
            return (float(centroide["latitude"]), float(centroide["longitude"]))
        return (None, None)
    except Exception as exc:
        logger.error("Erro ao obter coordenadas de %s: %s", co_ibge, exc)
        return (None, None)
