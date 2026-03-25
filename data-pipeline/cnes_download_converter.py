"""
cnes_download_converter.py
Baixa arquivo CNES por UF e converte CSV interno para JSON.
"""

from __future__ import annotations

import csv
import json
import logging
import os
import zipfile

from clients.requests_config import session

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

DATASUS_BASE_URL = "https://cnes.datasus.gov.br/services/estabelecimentos-exportar"
OUTPUT_DIR = os.environ.get("OUTPUT_DIR", "data/cnes")
DOWNLOAD_CHUNK_SIZE = 64 * 1024


def download_cnes_file(uf: str, dest_dir: str) -> str:
    """Baixa o arquivo CNES de uma UF em modo streaming."""
    os.makedirs(dest_dir, exist_ok=True)
    url = f"{DATASUS_BASE_URL}?estados={uf}&colunas=NO_RAZAO_SOCIAL,CO_CNES,CO_MUNICIPIO_GESTOR,TP_UNIDADE"
    dest_path = os.path.join(dest_dir, f"cnes_{uf}.zip")

    if os.path.exists(dest_path):
        logger.info("Arquivo ja existe, pulando download: %s", dest_path)
        return dest_path

    logger.info("Baixando CNES de %s -> %s", uf, dest_path)
    with session.get(url, timeout=120, stream=True) as response:
        response.raise_for_status()
        with open(dest_path, "wb") as f:
            for chunk in response.iter_content(chunk_size=DOWNLOAD_CHUNK_SIZE):
                if chunk:
                    f.write(chunk)

    logger.info("Download concluido: %s", dest_path)
    return dest_path


def convert_csv_to_json(zip_path: str, output_dir: str) -> str:
    """Extrai o primeiro CSV do ZIP e converte para JSON."""
    os.makedirs(output_dir, exist_ok=True)

    with zipfile.ZipFile(zip_path, "r") as zf:
        csv_names = [n for n in zf.namelist() if n.lower().endswith(".csv")]
        if not csv_names:
            raise ValueError(f"Nenhum CSV encontrado em {zip_path}")

        csv_name = csv_names[0]
        with zf.open(csv_name) as csv_file:
            reader = csv.DictReader(
                (line.decode("latin-1") for line in csv_file),
                delimiter=";",
            )
            records = list(reader)

    base_name = os.path.splitext(os.path.basename(zip_path))[0]
    json_path = os.path.join(output_dir, f"{base_name}.json")
    with open(json_path, "w", encoding="utf-8") as jf:
        json.dump(records, jf, ensure_ascii=False, indent=2)

    logger.info("Convertido para JSON: %s (%d registros)", json_path, len(records))
    return json_path


def process_uf(uf: str) -> None:
    zip_path = download_cnes_file(uf, OUTPUT_DIR)
    convert_csv_to_json(zip_path, OUTPUT_DIR)


if __name__ == "__main__":
    import sys

    ufs = sys.argv[1:] if len(sys.argv) > 1 else ["SP"]
    for uf in ufs:
        process_uf(uf.upper())
