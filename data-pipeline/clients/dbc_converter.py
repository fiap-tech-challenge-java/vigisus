"""
clients/dbc_converter.py
Conversor de arquivos DBC (formato proprietário DATASUS) para pandas DataFrame.
Tenta múltiplas bibliotecas em ordem de preferência.
"""

import logging
import os

import pandas as pd

logger = logging.getLogger(__name__)


def converter(arquivo_dbc: str) -> pd.DataFrame | None:
    """
    Converte um arquivo DBC para DataFrame.

    Tenta as seguintes estratégias em ordem:
    1. pysus  — lê DBC diretamente.
    2. blast-dbf + simpledbf — descomprime DBC → DBF → DataFrame.
    3. dbfread — lê DBF derivado (se existir).

    :param arquivo_dbc: Caminho para o arquivo .dbc.
    :return: DataFrame com os dados ou None se todas as tentativas falharem.
    """
    # ---------- 1. pysus ----------
    try:
        import pysus.files.databases  # noqa: F401 — verifica disponibilidade
        from pysus.utilities.readdbc import read_dbc

        df = read_dbc(arquivo_dbc, encoding="latin-1")
        logger.info("pysus: %d registros convertidos de %s", len(df), arquivo_dbc)
        return df
    except ImportError:
        logger.debug("pysus não disponível, tentando blast-dbf...")
    except Exception as exc:
        logger.warning("pysus falhou para %s: %s", arquivo_dbc, exc)

    # ---------- 2. blast-dbf + simpledbf ----------
    dbf_path = os.path.splitext(arquivo_dbc)[0] + ".dbf"
    try:
        import blast  # blast-dbf
        from simpledbf import Dbf5

        blast.decompress(arquivo_dbc, dbf_path)
        df = Dbf5(dbf_path).to_dataframe()
        logger.info("blast-dbf: %d registros convertidos de %s", len(df), arquivo_dbc)
        return df
    except ImportError:
        logger.debug("blast-dbf/simpledbf não disponíveis, tentando dbfread...")
    except Exception as exc:
        logger.warning("blast-dbf falhou para %s: %s", arquivo_dbc, exc)

    # ---------- 3. dbfread (requer .dbf com mesmo nome) ----------
    try:
        from dbfread import DBF

        if not os.path.exists(dbf_path):
            logger.warning("dbfread: arquivo DBF não encontrado em %s", dbf_path)
            raise FileNotFoundError(dbf_path)

        table = DBF(dbf_path, encoding="latin-1")
        df = pd.DataFrame(iter(table))
        logger.info("dbfread: %d registros convertidos de %s", len(df), dbf_path)
        return df
    except ImportError:
        logger.debug("dbfread não disponível.")
    except Exception as exc:
        logger.warning("dbfread falhou para %s: %s", dbf_path, exc)

    # ---------- Todas as estratégias falharam ----------
    logger.error(
        "Não foi possível converter %s. Instale uma das bibliotecas: "
        "pip install pysus  |  pip install blast-dbf simpledbf  |  pip install dbfread",
        arquivo_dbc,
    )
    print("Instale: pip install pysus")
    return None


def converter_e_salvar(
    arquivo_dbc: str,
    csv_destino: str,
) -> pd.DataFrame | None:
    """
    Converte um DBC para DataFrame e salva como CSV.

    :param arquivo_dbc: Caminho para o arquivo .dbc.
    :param csv_destino: Caminho de destino para o CSV.
    :return: DataFrame ou None se a conversão falhar.
    """
    df = converter(arquivo_dbc)
    if df is None:
        return None

    os.makedirs(os.path.dirname(os.path.abspath(csv_destino)), exist_ok=True)
    df.to_csv(csv_destino, index=False, encoding="utf-8")
    logger.info("CSV salvo em: %s", csv_destino)
    return df
