"""
ingest_sinan_dengue.py
Baixa DENGBR{AA}.dbc dos últimos 3 anos + preliminar do ano atual do FTP do
DATASUS, agrupa por (co_municipio, ano, semana_epidemiologica) e faz upsert
na tabela casos_dengue.
"""

import ftplib
import logging
import os
from datetime import date

import pandas as pd
from sqlalchemy import create_engine, text

from config import DATA_DIR, DB_URL, FTP_HOST

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

FTP_SINAN_PATH = "/dissemin/publicos/SINAN/DADOS/FINAIS"
FTP_SINAN_PRELIM_PATH = "/dissemin/publicos/SINAN/DADOS/PRELIM"


def _sinan_years() -> list:
    """Retorna os últimos 3 anos finalizados + ano atual."""
    current_year = date.today().year
    years = list(range(current_year - 3, current_year))  # 3 anos finais
    years.append(current_year)  # preliminar do ano atual
    return years


def _local_path(year: int) -> str:
    os.makedirs(DATA_DIR, exist_ok=True)
    aa = str(year)[-2:]
    return os.path.join(DATA_DIR, f"DENGBR{aa}.dbc")


def _ftp_download(year: int, local_path: str) -> bool:
    """
    Baixa o arquivo do FTP. Tenta primeiro em FINAIS, depois em PRELIM.
    Retorna True se o download foi realizado.
    """
    if os.path.exists(local_path):
        logger.info("Arquivo já existe localmente: %s", local_path)
        return True

    filename = os.path.basename(local_path)
    current_year = date.today().year
    paths_to_try = (
        [FTP_SINAN_PRELIM_PATH, FTP_SINAN_PATH]
        if year == current_year
        else [FTP_SINAN_PATH, FTP_SINAN_PRELIM_PATH]
    )

    for ftp_path in paths_to_try:
        try:
            logger.info("Tentando baixar %s de %s/%s...", filename, FTP_HOST, ftp_path)
            with ftplib.FTP(FTP_HOST, timeout=120) as ftp:
                ftp.login()
                ftp.cwd(ftp_path)
                with open(local_path, "wb") as f:
                    ftp.retrbinary(f"RETR {filename}", f.write)
            logger.info("Download concluído: %s", local_path)
            return True
        except ftplib.error_perm as exc:
            logger.warning("Arquivo não encontrado em %s: %s", ftp_path, exc)
            if os.path.exists(local_path):
                os.remove(local_path)

    return False


def _read_dbc(local_path: str) -> pd.DataFrame:
    """Lê o arquivo DBC e retorna um DataFrame."""
    from pysus.utilities.readdbc import read_dbc

    return read_dbc(local_path, encoding="latin-1")


def _aggregate(df: pd.DataFrame) -> pd.DataFrame:
    """Agrupa por (co_municipio, ano, semana_epidemiologica) e conta casos."""
    df.columns = [c.upper() for c in df.columns]

    # Coluna de município pode variar entre versões do SINAN
    mun_col = next(
        (c for c in ["ID_MUNICIP", "CO_MUN_NOT", "CO_MUNICIPIO"] if c in df.columns),
        None,
    )
    sem_col = next(
        (c for c in ["SEM_NOT", "SEM_PRIM", "SEMANA_EPI"] if c in df.columns),
        None,
    )
    dt_col = next(
        (c for c in ["DT_NOTIFIC", "DT_SIN_PRI"] if c in df.columns),
        None,
    )

    if not mun_col or not sem_col:
        raise ValueError(
            f"Colunas esperadas não encontradas. Disponíveis: {list(df.columns)}"
        )

    df = df[[mun_col, sem_col, dt_col]].copy() if dt_col else df[[mun_col, sem_col]].copy()
    df.rename(
        columns={mun_col: "co_municipio", sem_col: "semana_epidemiologica"},
        inplace=True,
    )

    if dt_col:
        df["ano"] = pd.to_numeric(
            df[dt_col].astype(str).str[:4], errors="coerce"
        )
    else:
        df["ano"] = df["semana_epidemiologica"].astype(str).str[:4].pipe(
            pd.to_numeric, errors="coerce"
        )

    df["semana_epidemiologica"] = (
        df["semana_epidemiologica"].astype(str).str[-2:].pipe(pd.to_numeric, errors="coerce")
    )
    df = df.dropna(subset=["co_municipio", "ano", "semana_epidemiologica"])
    df["co_municipio"] = df["co_municipio"].astype(str)
    df["ano"] = df["ano"].astype(int)
    df["semana_epidemiologica"] = df["semana_epidemiologica"].astype(int)

    grouped = (
        df.groupby(["co_municipio", "ano", "semana_epidemiologica"])
        .size()
        .reset_index(name="total_casos")
    )
    return grouped


def _upsert(engine, df: pd.DataFrame) -> int:
    """Faz upsert dos casos de dengue agregados."""
    sql = text(
        """
        INSERT INTO casos_dengue (co_municipio, ano, semana_epidemiologica, total_casos)
        VALUES (:co_municipio, :ano, :semana_epidemiologica, :total_casos)
        ON CONFLICT (co_municipio, ano, semana_epidemiologica) DO UPDATE
            SET total_casos = EXCLUDED.total_casos
        """
    )
    records = df.to_dict(orient="records")
    with engine.begin() as conn:
        conn.execute(sql, records)
    return len(records)


def run() -> None:
    engine = create_engine(DB_URL)
    years = _sinan_years()

    for year in years:
        local_path = _local_path(year)
        downloaded = _ftp_download(year, local_path)
        if not downloaded:
            logger.warning("Arquivo não disponível para o ano %d — ignorando.", year)
            continue
        try:
            df = _read_dbc(local_path)
            aggregated = _aggregate(df)
            total = _upsert(engine, aggregated)
            logger.info("Importados %d casos de dengue (%d)", total, year)
        except Exception as exc:
            logger.error("Erro ao processar dengue %d: %s", year, exc)


if __name__ == "__main__":
    run()
