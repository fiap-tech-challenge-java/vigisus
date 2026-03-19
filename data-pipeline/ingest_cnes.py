"""
ingest_cnes.py
Baixa 3 arquivos DBC do FTP do DATASUS e importa no banco:
  ST{UF}{COMPETENCIA}.dbc → tabela estabelecimentos
  LT{UF}{COMPETENCIA}.dbc → tabela leitos
  SR{UF}{COMPETENCIA}.dbc → tabela servicos_especializados
"""

import ftplib
import logging
import os

import pandas as pd
from pysus.ftp.databases.cnes import CNES
from sqlalchemy import create_engine, text

from config import COMPETENCIA, DATA_DIR, DB_URL, FTP_HOST, UF

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

CNES_FILES = {
    "ST": {
        "ftp_path": "/dissemin/publicos/CNES/200508_/Dados/ST",
        "table": "estabelecimentos",
        "columns": [
            "CO_CNES",
            "NO_FANTASIA",
            "CO_MUNICIPIO_GESTOR",
            "NU_LATITUDE",
            "NU_LONGITUDE",
            "NU_TELEFONE",
            "TP_GESTAO",
        ],
        "pk": "co_cnes",
    },
    "LT": {
        "ftp_path": "/dissemin/publicos/CNES/200508_/Dados/LT",
        "table": "leitos",
        "columns": ["CNES", "CODUFMUN", "CODLEITO", "DSLEITO", "QT_EXIST", "QT_SUS"],
        "pk": "cnes",
    },
    "SR": {
        "ftp_path": "/dissemin/publicos/CNES/200508_/Dados/SR",
        "table": "servicos_especializados",
        "columns": ["CNES", "SERV_ESP", "CLASS_SR"],
        "pk": "cnes",
    },
}


def _local_path(prefix: str) -> str:
    """Retorna o caminho local esperado para o arquivo DBC."""
    os.makedirs(DATA_DIR, exist_ok=True)
    return os.path.join(DATA_DIR, f"{prefix}{UF}{COMPETENCIA}.dbc")


def _ftp_download(prefix: str, ftp_path: str, local_path: str) -> None:
    """Baixa o arquivo DBC do FTP se ainda não existir localmente."""
    if os.path.exists(local_path):
        logger.info("Arquivo já existe localmente: %s", local_path)
        return

    filename = os.path.basename(local_path)
    logger.info("Baixando %s do FTP %s...", filename, FTP_HOST)
    with ftplib.FTP(FTP_HOST, timeout=120) as ftp:
        ftp.login()
        ftp.cwd(ftp_path)
        with open(local_path, "wb") as f:
            ftp.retrbinary(f"RETR {filename}", f.write)
    logger.info("Download concluído: %s", local_path)


def _read_dbc(local_path: str) -> pd.DataFrame:
    """Converte o arquivo DBC para DataFrame usando pysus."""
    from pysus.utilities.readdbc import read_dbc

    return read_dbc(local_path, encoding="latin-1")


def _normalize_columns(df: pd.DataFrame, desired_cols: list) -> pd.DataFrame:
    """Seleciona apenas as colunas desejadas (ignora ausentes)."""
    df.columns = [c.upper() for c in df.columns]
    available = [c for c in desired_cols if c in df.columns]
    if not available:
        raise ValueError(f"Nenhuma das colunas {desired_cols} encontrada no DataFrame")
    return df[available].copy()


def _upsert(engine, df: pd.DataFrame, table: str, pk: str) -> int:
    """Faz upsert do DataFrame na tabela especificada."""
    cols = list(df.columns)
    col_names = ", ".join(f'"{c.lower()}"' for c in cols)
    placeholders = ", ".join(f":{c.lower()}" for c in cols)
    updates = ", ".join(
        f'"{c.lower()}" = EXCLUDED."{c.lower()}"'
        for c in cols
        if c.lower() != pk
    )

    sql = text(
        f"""
        INSERT INTO {table} ({col_names})
        VALUES ({placeholders})
        ON CONFLICT ("{pk}") DO UPDATE SET {updates}
        """
    )

    records = df.rename(columns=str.lower).to_dict(orient="records")
    with engine.begin() as conn:
        conn.execute(sql, records)
    return len(records)


def ingest_prefix(prefix: str, engine) -> int:
    """Baixa, converte e importa um arquivo CNES (ST, LT ou SR)."""
    cfg = CNES_FILES[prefix]
    local_path = _local_path(prefix)
    _ftp_download(prefix, cfg["ftp_path"], local_path)
    df = _read_dbc(local_path)
    df = _normalize_columns(df, cfg["columns"])
    total = _upsert(engine, df, cfg["table"], cfg["pk"])
    return total


def run() -> None:
    engine = create_engine(DB_URL)

    n_est = ingest_prefix("ST", engine)
    logger.info("Importados %d estabelecimentos", n_est)

    n_lei = ingest_prefix("LT", engine)
    logger.info("Importados %d leitos", n_lei)

    n_srv = ingest_prefix("SR", engine)
    logger.info("Importados %d serviços especializados", n_srv)

    logger.info(
        "Importados %d estabelecimentos / %d leitos / %d serviços",
        n_est,
        n_lei,
        n_srv,
    )


if __name__ == "__main__":
    run()
