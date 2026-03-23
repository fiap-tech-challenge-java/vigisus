"""
ingest_planilhas_saude.py
Lê arquivos CSV/Excel (ST, LT, SR, DENG) e persiste no PostgreSQL.

Mapeamento esperado por prefixo do arquivo:
  - ST   -> estabelecimentos
  - LT   -> leitos
  - SR   -> servicos_especializados
  - DENG -> casos_dengue (agregado por município/ano/semana)

Regra de substituição:
  - Remove registros existentes (incluindo dados mockados) nas tabelas de destino
    e recarrega os dados provenientes das planilhas.
"""

import csv
import logging
import os
from collections import defaultdict
from pathlib import Path

import pandas as pd
from sqlalchemy import create_engine, text

from config import DB_URL

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

SPREADSHEET_DIR = os.environ.get("SPREADSHEET_DIR", "csv_input")
SPREADSHEET_FALLBACK_DIR = os.environ.get("SPREADSHEET_FALLBACK_DIR", "data/csv")

SUPPORTED_EXTENSIONS = {".csv", ".xlsx", ".xls"}

ALIASES = {
    "co_cnes": ["CO_CNES", "CNES"],
    "no_fantasia": ["NO_FANTASIA", "NOME_FANTASIA", "FANTASIA"],
    "co_municipio": ["CO_MUNICIPIO_GESTOR", "CO_MUNICIPIO", "CODUFMUN", "ID_MUNICIP", "CO_MUN_NOT"],
    "nu_latitude": ["NU_LATITUDE", "LATITUDE", "LAT"],
    "nu_longitude": ["NU_LONGITUDE", "LONGITUDE", "LON", "LNG"],
    "nu_telefone": ["NU_TELEFONE", "TELEFONE"],
    "tp_gestao": ["TP_GESTAO", "TPGESTAO"],
    "competencia": ["COMPETEN", "COMPETENCIA"],
    "tp_leito": ["TP_LEITO", "CODLEITO"],
    "ds_leito": ["DS_LEITO", "DSLEITO"],
    "qt_exist": ["QT_EXIST"],
    "qt_sus": ["QT_SUS"],
    "serv_esp": ["SERV_ESP"],
    "class_sr": ["CLASS_SR"],
    "ano": ["NU_ANO", "ANO", "YEAR"],
    "semana_epi": ["SEM_NOT", "SEMANA_EPI", "SEMANA_EPIDEMIOLOGICA", "SEM_PRI"],
    "dt_notific": ["DT_NOTIFIC", "DT_SIN_PRI"],
}


def _normalize_col(col: str) -> str:
    return "".join(ch for ch in str(col).strip().upper() if ch.isalnum() or ch == "_")


def _read_columns(path: Path) -> list[str]:
    if path.suffix.lower() == ".csv":
        with path.open("r", encoding="utf-8-sig", newline="") as fh:
            sample = fh.read(4096)
            fh.seek(0)
            try:
                dialect = csv.Sniffer().sniff(sample, delimiters=",;\t|")
                reader = csv.reader(fh, delimiter=dialect.delimiter)
            except csv.Error:
                reader = csv.reader(fh, delimiter=",")
            return next(reader)

    df = pd.read_excel(path, nrows=0)
    return [str(c) for c in df.columns]


def _build_column_lookup(path: Path) -> tuple[dict[str, str], list[str]]:
    original_cols = _read_columns(path)
    normalized_lookup = {_normalize_col(c): c for c in original_cols}

    mapped = {}
    for canonical, aliases in ALIASES.items():
        for alias in aliases:
            key = _normalize_col(alias)
            if key in normalized_lookup:
                mapped[canonical] = normalized_lookup[key]
                break

    return mapped, original_cols


def _read_dataframe(path: Path, use_columns: list[str]) -> pd.DataFrame:
    if path.suffix.lower() == ".csv":
        return pd.read_csv(path, usecols=use_columns, dtype=str, low_memory=False)
    return pd.read_excel(path, usecols=use_columns, dtype=str)


def _find_source_files() -> dict[str, list[Path]]:
    by_prefix = defaultdict(list)

    search_roots = []
    primary = Path(SPREADSHEET_DIR)
    fallback = Path(SPREADSHEET_FALLBACK_DIR)

    if primary.exists():
        search_roots.append(primary)
    if fallback.exists() and fallback.resolve() != primary.resolve():
        search_roots.append(fallback)

    for root in search_roots:
        for path in root.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in SUPPORTED_EXTENSIONS:
                continue
            upper_name = path.name.upper()
            if upper_name.startswith("ST"):
                by_prefix["ST"].append(path)
            elif upper_name.startswith("LT"):
                by_prefix["LT"].append(path)
            elif upper_name.startswith("SR"):
                by_prefix["SR"].append(path)
            elif upper_name.startswith("DENG") or upper_name.startswith("DENGBR"):
                by_prefix["DENG"].append(path)

    for key in by_prefix:
        by_prefix[key] = sorted(by_prefix[key])

    return by_prefix


def _to_nullable_float(value):
    if value is None:
        return None
    value = str(value).strip()
    if not value:
        return None
    value = value.replace(",", ".")
    try:
        return float(value)
    except ValueError:
        return None


def _to_nullable_int(value):
    if value is None:
        return None
    value = str(value).strip()
    if not value:
        return None
    try:
        return int(float(value))
    except ValueError:
        return None


def _normalize_competencia(value):
    if value is None:
        return None
    txt = str(value).strip()
    digits = "".join(ch for ch in txt if ch.isdigit())
    return digits[:6] if digits else None


def _map_codigo_municipio(raw_code: str | None, resolver) -> str | None:
    if raw_code is None:
        return None
    raw_digits = "".join(ch for ch in str(raw_code) if ch.isdigit())
    if not raw_digits:
        return None
    return resolver(raw_digits)


def _build_municipio_resolver(conn):
    all_codes = {
        row[0]
        for row in conn.execute(text("SELECT co_ibge FROM municipios"))
        if row and row[0]
    }

    prefix_index = defaultdict(list)
    for code in all_codes:
        prefix_index[code[:6]].append(code)

    cache = {}

    def resolve(code: str) -> str | None:
        if code in cache:
            return cache[code]

        if len(code) == 7 and code in all_codes:
            cache[code] = code
            return code

        if len(code) >= 6:
            prefix = code[:6]
            candidates = prefix_index.get(prefix, [])
            if len(candidates) == 1:
                cache[code] = candidates[0]
                return candidates[0]
            if code in candidates:
                cache[code] = code
                return code

        cache[code] = None
        return None

    return resolve


def _prepare_estabelecimentos(paths: list[Path], resolver) -> list[dict]:
    records = {}
    for path in paths:
        mapped, available = _build_column_lookup(path)
        required = ["co_cnes", "co_municipio"]
        if not all(k in mapped for k in required):
            logger.warning("Arquivo ST ignorado (colunas mínimas ausentes): %s", path)
            continue

        use_cols = [mapped[k] for k in mapped]
        df = _read_dataframe(path, use_cols)
        rename_map = {v: k for k, v in mapped.items()}
        df = df.rename(columns=rename_map)

        for _, row in df.iterrows():
            co_cnes = "".join(ch for ch in str(row.get("co_cnes", "")) if ch.isdigit())
            co_cnes = co_cnes.zfill(7) if co_cnes else None
            if not co_cnes:
                continue

            co_municipio = _map_codigo_municipio(row.get("co_municipio"), resolver)
            if not co_municipio:
                continue

            records[co_cnes] = {
                "co_cnes": co_cnes,
                "no_fantasia": (str(row.get("no_fantasia", "")).strip() or None),
                "co_municipio": co_municipio,
                "nu_latitude": _to_nullable_float(row.get("nu_latitude")),
                "nu_longitude": _to_nullable_float(row.get("nu_longitude")),
                "nu_telefone": (str(row.get("nu_telefone", "")).strip() or None),
                "tp_gestao": (str(row.get("tp_gestao", "")).strip() or None),
                "competencia": _normalize_competencia(row.get("competencia")),
            }

        logger.info("ST processado: %s (%d linhas, %d colunas)", path, len(df), len(available))

    return list(records.values())


def _prepare_leitos(paths: list[Path], resolver) -> list[dict]:
    out = []
    for path in paths:
        mapped, available = _build_column_lookup(path)
        required = ["co_cnes", "tp_leito"]
        if not all(k in mapped for k in required):
            logger.warning("Arquivo LT ignorado (colunas mínimas ausentes): %s", path)
            continue

        use_cols = [mapped[k] for k in mapped]
        df = _read_dataframe(path, use_cols)
        df = df.rename(columns={v: k for k, v in mapped.items()})

        for _, row in df.iterrows():
            co_cnes = "".join(ch for ch in str(row.get("co_cnes", "")) if ch.isdigit())
            co_cnes = co_cnes.zfill(7) if co_cnes else None
            if not co_cnes:
                continue

            out.append(
                {
                    "co_cnes": co_cnes,
                    "tp_leito": (str(row.get("tp_leito", "")).strip() or None),
                    "ds_leito": (str(row.get("ds_leito", "")).strip() or None),
                    "qt_exist": _to_nullable_int(row.get("qt_exist")),
                    "qt_sus": _to_nullable_int(row.get("qt_sus")),
                    "competencia": _normalize_competencia(row.get("competencia")),
                }
            )

        logger.info("LT processado: %s (%d linhas, %d colunas)", path, len(df), len(available))

    return out


def _prepare_servicos(paths: list[Path], resolver) -> list[dict]:
    out = []
    for path in paths:
        mapped, available = _build_column_lookup(path)
        required = ["co_cnes", "serv_esp", "class_sr"]
        if not all(k in mapped for k in required):
            logger.warning("Arquivo SR ignorado (colunas mínimas ausentes): %s", path)
            continue

        use_cols = [mapped[k] for k in mapped]
        df = _read_dataframe(path, use_cols)
        df = df.rename(columns={v: k for k, v in mapped.items()})

        for _, row in df.iterrows():
            co_cnes = "".join(ch for ch in str(row.get("co_cnes", "")) if ch.isdigit())
            co_cnes = co_cnes.zfill(7) if co_cnes else None
            if not co_cnes:
                continue

            out.append(
                {
                    "co_cnes": co_cnes,
                    "serv_esp": (str(row.get("serv_esp", "")).strip() or None),
                    "class_sr": (str(row.get("class_sr", "")).strip() or None),
                    "competencia": _normalize_competencia(row.get("competencia")),
                }
            )

        logger.info("SR processado: %s (%d linhas, %d colunas)", path, len(df), len(available))

    return out


def _extract_ano_semana(row: dict) -> tuple[int | None, int | None]:
    ano = _to_nullable_int(row.get("ano"))
    semana_raw = row.get("semana_epi")

    if semana_raw is not None:
        txt = "".join(ch for ch in str(semana_raw) if ch.isdigit())
        if len(txt) >= 6:
            if ano is None:
                ano = _to_nullable_int(txt[:4])
            semana = _to_nullable_int(txt[-2:])
            return ano, semana
        semana = _to_nullable_int(txt)
    else:
        semana = None

    if ano is None:
        dt_val = row.get("dt_notific")
        digits = "".join(ch for ch in str(dt_val) if ch.isdigit()) if dt_val is not None else ""
        if len(digits) >= 4:
            ano = _to_nullable_int(digits[:4])

    return ano, semana


def _aggregate_dengue(paths: list[Path], resolver) -> list[dict]:
    grouped = defaultdict(int)

    for path in paths:
        mapped, available = _build_column_lookup(path)
        required = ["co_municipio", "semana_epi"]
        if not all(k in mapped for k in required):
            logger.warning("Arquivo DENG ignorado (colunas mínimas ausentes): %s", path)
            continue

        use_cols = [mapped[k] for k in mapped]
        if path.suffix.lower() == ".csv":
            iterator = pd.read_csv(
                path,
                usecols=use_cols,
                dtype=str,
                low_memory=False,
                chunksize=200_000,
            )
        else:
            iterator = [pd.read_excel(path, usecols=use_cols, dtype=str)]

        total_rows = 0
        for chunk in iterator:
            chunk = chunk.rename(columns={v: k for k, v in mapped.items()})
            total_rows += len(chunk)
            for _, row in chunk.iterrows():
                co_municipio = _map_codigo_municipio(row.get("co_municipio"), resolver)
                if not co_municipio:
                    continue

                ano, semana = _extract_ano_semana(row)
                if ano is None or semana is None or semana <= 0 or semana > 53:
                    continue

                grouped[(co_municipio, ano, semana)] += 1

        logger.info("DENG processado: %s (%d linhas, %d colunas)", path, total_rows, len(available))

    return [
        {
            "co_municipio": co_municipio,
            "ano": ano,
            "semana_epi": semana,
            "total_casos": total,
        }
        for (co_municipio, ano, semana), total in grouped.items()
    ]


def _replace_data(conn, estabelecimentos, leitos, servicos, casos_dengue):
    # Ordem de deleção respeita as FKs existentes.
    conn.execute(text("DELETE FROM leitos"))
    conn.execute(text("DELETE FROM servicos_especializados"))
    conn.execute(text("DELETE FROM estabelecimentos"))
    conn.execute(text("DELETE FROM casos_dengue"))

    if estabelecimentos:
        conn.execute(
            text(
                """
                INSERT INTO estabelecimentos
                    (co_cnes, no_fantasia, co_municipio,
                     nu_latitude, nu_longitude, nu_telefone, tp_gestao, competencia)
                VALUES
                    (:co_cnes, :no_fantasia, :co_municipio,
                     :nu_latitude, :nu_longitude, :nu_telefone, :tp_gestao, :competencia)
                """
            ),
            estabelecimentos,
        )

    if leitos:
        conn.execute(
            text(
                """
                INSERT INTO leitos
                    (co_cnes, tp_leito, ds_leito, qt_exist, qt_sus, competencia)
                VALUES
                    (:co_cnes, :tp_leito, :ds_leito, :qt_exist, :qt_sus, :competencia)
                """
            ),
            leitos,
        )

    if servicos:
        conn.execute(
            text(
                """
                INSERT INTO servicos_especializados
                    (co_cnes, serv_esp, class_sr, competencia)
                VALUES
                    (:co_cnes, :serv_esp, :class_sr, :competencia)
                """
            ),
            servicos,
        )

    if casos_dengue:
        conn.execute(
            text(
                """
                INSERT INTO casos_dengue
                    (co_municipio, ano, semana_epi, total_casos)
                VALUES
                    (:co_municipio, :ano, :semana_epi, :total_casos)
                """
            ),
            casos_dengue,
        )


def run() -> None:
    files = _find_source_files()
    logger.info(
        "Arquivos localizados: ST=%d LT=%d SR=%d DENG=%d",
        len(files.get("ST", [])),
        len(files.get("LT", [])),
        len(files.get("SR", [])),
        len(files.get("DENG", [])),
    )

    engine = create_engine(DB_URL)
    with engine.begin() as conn:
        resolver = _build_municipio_resolver(conn)

        estabelecimentos = _prepare_estabelecimentos(files.get("ST", []), resolver)
        leitos = _prepare_leitos(files.get("LT", []), resolver)
        servicos = _prepare_servicos(files.get("SR", []), resolver)
        casos_dengue = _aggregate_dengue(files.get("DENG", []), resolver)

        _replace_data(conn, estabelecimentos, leitos, servicos, casos_dengue)

    logger.info(
        "Ingestão concluída: %d estabelecimentos, %d leitos, %d serviços, %d casos agregados",
        len(estabelecimentos),
        len(leitos),
        len(servicos),
        len(casos_dengue),
    )


if __name__ == "__main__":
    run()