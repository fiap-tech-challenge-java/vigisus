"""
requests_config.py
Factory de sessao HTTP padronizada para o pipeline.

Melhorias:
- retry automatico para erros transientes (429/5xx);
- pool de conexoes reutilizavel;
- SSL verificavel por variavel de ambiente.
"""

from __future__ import annotations

import os

import requests
import urllib3
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

DEFAULT_RETRY_TOTAL = int(os.environ.get("HTTP_RETRY_TOTAL", "3"))
DEFAULT_RETRY_BACKOFF = float(os.environ.get("HTTP_RETRY_BACKOFF", "0.5"))
VERIFY_SSL = os.environ.get("REQUESTS_VERIFY_SSL", "true").strip().lower() not in {
    "0",
    "false",
    "no",
}


def _build_retry_policy() -> Retry:
    return Retry(
        total=DEFAULT_RETRY_TOTAL,
        connect=DEFAULT_RETRY_TOTAL,
        read=DEFAULT_RETRY_TOTAL,
        status=DEFAULT_RETRY_TOTAL,
        backoff_factor=DEFAULT_RETRY_BACKOFF,
        status_forcelist=[429, 500, 502, 503, 504],
        allowed_methods=frozenset({"HEAD", "GET", "POST", "PUT", "DELETE", "OPTIONS"}),
        raise_on_status=False,
    )


def create_session(verify_ssl: bool | None = None) -> requests.Session:
    """
    Cria uma requests.Session com retry/pool configurados.

    :param verify_ssl: sobrescreve a configuracao global de SSL.
    """
    should_verify_ssl = VERIFY_SSL if verify_ssl is None else verify_ssl
    session = requests.Session()

    adapter = HTTPAdapter(
        max_retries=_build_retry_policy(),
        pool_connections=20,
        pool_maxsize=20,
    )
    session.mount("https://", adapter)
    session.mount("http://", adapter)
    session.verify = should_verify_ssl

    if not should_verify_ssl:
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

    return session


# Sessao global para uso nos clientes.
session = create_session()
