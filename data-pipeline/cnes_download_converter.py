"""
cnes_download_converter.py
Baixa e converte arquivos do CNES (Cadastro Nacional de Estabelecimentos de Saúde)
do portal do DATASUS para formatos utilizáveis pelo pipeline de dados do Vigisus.
"""

import os
import csv
import json
import logging
import zipfile
import requests

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

DATASUS_BASE_URL = "https://cnes.datasus.gov.br/services/estabelecimentos-exportar"
OUTPUT_DIR = os.environ.get("OUTPUT_DIR", "data/cnes")


def download_cnes_file(uf: str, dest_dir: str) -> str:
    """Baixa o arquivo CNES de uma UF e salva no diretório de destino."""
    os.makedirs(dest_dir, exist_ok=True)
    url = f"{DATASUS_BASE_URL}?estados={uf}&colunas=NO_RAZAO_SOCIAL,CO_CNES,CO_MUNICIPIO_GESTOR,TP_UNIDADE"
    dest_path = os.path.join(dest_dir, f"cnes_{uf}.zip")
    logger.info("Baixando CNES de %s -> %s", uf, dest_path)
    response = requests.get(url, timeout=120)
    response.raise_for_status()
    with open(dest_path, "wb") as f:
        f.write(response.content)
    logger.info("Download concluído: %s", dest_path)
    return dest_path


def convert_csv_to_json(zip_path: str, output_dir: str) -> str:
    """Extrai o CSV do ZIP e converte para JSON."""
    os.makedirs(output_dir, exist_ok=True)
    with zipfile.ZipFile(zip_path, "r") as zf:
        csv_names = [n for n in zf.namelist() if n.endswith(".csv")]
        if not csv_names:
            raise ValueError(f"Nenhum CSV encontrado em {zip_path}")
        csv_name = csv_names[0]
        with zf.open(csv_name) as csv_file:
            reader = csv.DictReader(
                (line.decode("latin-1") for line in csv_file), delimiter=";"
            )
            records = list(reader)

    base_name = os.path.splitext(os.path.basename(zip_path))[0]
    json_path = os.path.join(output_dir, f"{base_name}.json")
    with open(json_path, "w", encoding="utf-8") as jf:
        json.dump(records, jf, ensure_ascii=False, indent=2)
    logger.info("Convertido para JSON: %s (%d registros)", json_path, len(records))
    return json_path


def process_uf(uf: str) -> None:
    """Pipeline completo de download e conversão para uma UF."""
    zip_path = download_cnes_file(uf, OUTPUT_DIR)
    convert_csv_to_json(zip_path, OUTPUT_DIR)


if __name__ == "__main__":
    import sys

    ufs = sys.argv[1:] if len(sys.argv) > 1 else ["SP"]
    for uf in ufs:
        process_uf(uf.upper())
