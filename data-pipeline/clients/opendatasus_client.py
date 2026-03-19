"""
clients/opendatasus_client.py
Cliente para o portal OpenDataSUS (CKAN) — dados abertos do Ministério da Saúde.
Permite pesquisar datasets, listar recursos CSV e fazer download de arquivos grandes.
"""

import logging
import os

import requests

logger = logging.getLogger(__name__)

_BASE_URL = "https://dadosabertos.saude.gov.br/api/3/action"
_TIMEOUT = 30
_CHUNK_SIZE = 8192


def listar_datasets(query: str = "dengue") -> list[dict]:
    """
    Pesquisa datasets no portal OpenDataSUS.

    :param query: Termo de busca (padrão 'dengue').
    :return: Lista de { titulo, atualizado_em, recursos: [{ nome, formato, url }] }.
    """
    url = f"{_BASE_URL}/package_search"
    params = {"q": query, "rows": 5}
    try:
        response = requests.get(url, params=params, timeout=_TIMEOUT)
        response.raise_for_status()
        data = response.json()
    except requests.RequestException as exc:
        logger.error("OpenDataSUS indisponível (listar_datasets): %s", exc)
        return []

    try:
        results = data.get("result", {}).get("results", [])
        datasets = []
        for pkg in results:
            recursos = [
                {
                    "nome": r.get("name") or r.get("description", ""),
                    "formato": r.get("format", ""),
                    "url": r.get("url", ""),
                }
                for r in pkg.get("resources", [])
            ]
            datasets.append({
                "titulo": pkg.get("title") or pkg.get("name", ""),
                "atualizado_em": pkg.get("metadata_modified", ""),
                "recursos": recursos,
            })
        return datasets
    except Exception as exc:
        logger.error("Erro ao parsear datasets: %s", exc)
        return []


def listar_recursos_csv(dataset_id: str) -> list[dict]:
    """
    Lista apenas os recursos CSV de um dataset.

    :param dataset_id: ID ou slug do dataset no CKAN.
    :return: Lista de { nome, url, atualizado_em }.
    """
    url = f"{_BASE_URL}/package_show"
    params = {"id": dataset_id}
    try:
        response = requests.get(url, params=params, timeout=_TIMEOUT)
        response.raise_for_status()
        data = response.json()
    except requests.RequestException as exc:
        logger.error("OpenDataSUS indisponível (listar_recursos_csv): %s", exc)
        return []

    try:
        resources = data.get("result", {}).get("resources", [])
        return [
            {
                "nome": r.get("name") or r.get("description", ""),
                "url": r.get("url", ""),
                "atualizado_em": r.get("last_modified") or r.get("created", ""),
            }
            for r in resources
            if r.get("format", "").upper() == "CSV"
        ]
    except Exception as exc:
        logger.error("Erro ao parsear recursos CSV de %s: %s", dataset_id, exc)
        return []


def download_csv(url: str, destino: str) -> bool:
    """
    Faz download de um arquivo CSV em chunks de 8 KB.

    Pula o download se o arquivo já existir localmente.
    Exibe progresso a cada MB baixado.

    :param url: URL pública do arquivo.
    :param destino: Caminho local de destino.
    :return: True se o download foi bem-sucedido (ou arquivo já existia).
    """
    if os.path.exists(destino):
        logger.info("Arquivo já existe, pulando download: %s", destino)
        return True

    os.makedirs(os.path.dirname(os.path.abspath(destino)), exist_ok=True)

    try:
        response = requests.get(url, stream=True, timeout=_TIMEOUT)
        response.raise_for_status()
    except requests.RequestException as exc:
        logger.error("Erro ao iniciar download de %s: %s", url, exc)
        return False

    total_bytes = 0
    last_reported_mb = 0

    try:
        with open(destino, "wb") as f:
            for chunk in response.iter_content(chunk_size=_CHUNK_SIZE):
                if chunk:
                    f.write(chunk)
                    total_bytes += len(chunk)
                    mb = total_bytes // (1024 * 1024)
                    if mb > last_reported_mb:
                        print(f"Baixando... {mb} MB")
                        last_reported_mb = mb
        logger.info("Download concluído: %s (%.2f MB)", destino, total_bytes / (1024 * 1024))
        return True
    except OSError as exc:
        logger.error("Erro ao salvar arquivo %s: %s", destino, exc)
        return False
