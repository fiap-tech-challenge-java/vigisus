"""
cnes_ftp_download.py
Baixa arquivos do CNES via FTP do DATASUS (ftp.datasus.gov.br).
Os arquivos são disponibilizados mensalmente no formato DBC compactado.
"""

import ftplib
import logging
import os

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

FTP_HOST = "ftp.datasus.gov.br"
FTP_CNES_PATH = "/dissemin/publicos/CNES/200508_/Dados/ST"
OUTPUT_DIR = os.environ.get("OUTPUT_DIR", "data/cnes_ftp")


def list_cnes_files(ftp: ftplib.FTP, remote_path: str) -> list:
    """Lista os arquivos disponíveis no diretório FTP do CNES."""
    ftp.cwd(remote_path)
    files = ftp.nlst()
    logger.info("Encontrados %d arquivos em %s", len(files), remote_path)
    return files


def download_file(ftp: ftplib.FTP, filename: str, dest_dir: str) -> str:
    """Baixa um arquivo do FTP para o diretório local."""
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, filename)
    logger.info("Baixando %s -> %s", filename, dest_path)
    with open(dest_path, "wb") as f:
        ftp.retrbinary(f"RETR {filename}", f.write)
    logger.info("Download concluído: %s", dest_path)
    return dest_path


def download_cnes_ftp(ufs: list = None, year_month: str = None) -> None:
    """
    Baixa arquivos CNES do FTP do DATASUS para as UFs e competência informadas.

    :param ufs: Lista de siglas de UF (ex.: ['SP', 'RJ']). None para todas.
    :param year_month: Competência no formato AAMM (ex.: '2401'). None para todos.
    """
    with ftplib.FTP(FTP_HOST, timeout=60) as ftp:
        ftp.login()
        logger.info("Conectado ao FTP: %s", FTP_HOST)
        files = list_cnes_files(ftp, FTP_CNES_PATH)

        for filename in files:
            if not filename.upper().endswith(".DBC"):
                continue
            uf_code = filename[2:4].upper()
            competencia = filename[4:8]
            if ufs and uf_code not in [u.upper() for u in ufs]:
                continue
            if year_month and competencia != year_month:
                continue
            download_file(ftp, filename, OUTPUT_DIR)


if __name__ == "__main__":
    import sys

    ufs_arg = sys.argv[1].split(",") if len(sys.argv) > 1 else None
    ym_arg = sys.argv[2] if len(sys.argv) > 2 else None
    download_cnes_ftp(ufs=ufs_arg, year_month=ym_arg)
