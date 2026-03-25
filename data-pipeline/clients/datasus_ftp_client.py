"""
clients/datasus_ftp_client.py
Cliente FTP para download de arquivos publicos do DATASUS.
"""

from __future__ import annotations

import ftplib
import logging
import os

logger = logging.getLogger(__name__)

_FTP_HOST = "ftp.datasus.gov.br"
_FTP_TIMEOUT = 60

PASTAS_FTP = {
    "SINAN_FINAL": "/dissemin/publicos/SINAN/DADOS/FINAIS/",
    "SINAN_PRELIM": "/dissemin/publicos/SINAN/DADOS/PRELIM/",
    "CNES_ST": "/dissemin/publicos/CNES/200508_/Dados/ST/",
    "CNES_LT": "/dissemin/publicos/CNES/200508_/Dados/LT/",
    "CNES_SR": "/dissemin/publicos/CNES/200508_/Dados/SR/",
}


def _connect() -> ftplib.FTP:
    ftp = ftplib.FTP(timeout=_FTP_TIMEOUT)
    ftp.connect(_FTP_HOST)
    ftp.login()  # login anonimo
    return ftp


def listar_arquivos(pasta: str) -> list[str]:
    try:
        with _connect() as ftp:
            ftp.cwd(pasta)
            arquivos = ftp.nlst()
            logger.info("FTP %s: %d arquivos encontrados", pasta, len(arquivos))
            return arquivos
    except ftplib.all_errors as exc:
        logger.error("Erro FTP ao listar %s: %s", pasta, exc)
        return []


def baixar_arquivo(
    pasta: str,
    arquivo: str,
    destino_dir: str = "./downloads",
) -> str | None:
    os.makedirs(destino_dir, exist_ok=True)
    destino_path = os.path.join(destino_dir, arquivo)

    if os.path.exists(destino_path):
        logger.info("Arquivo ja existe, pulando: %s", destino_path)
        return destino_path

    total_bytes = [0]
    last_reported_mb = [0]

    def _callback(chunk: bytes) -> None:
        total_bytes[0] += len(chunk)
        mb = total_bytes[0] // (1024 * 1024)
        if mb > last_reported_mb[0]:
            print(f"Baixando {arquivo}... {mb} MB")
            last_reported_mb[0] = mb

    try:
        with _connect() as ftp:
            ftp.cwd(pasta)
            with open(destino_path, "wb") as f:
                def _write(chunk: bytes) -> None:
                    f.write(chunk)
                    _callback(chunk)

                ftp.retrbinary(f"RETR {arquivo}", _write)
        logger.info(
            "Download concluido: %s (%.2f MB)",
            destino_path,
            total_bytes[0] / (1024 * 1024),
        )
        return destino_path
    except ftplib.all_errors as exc:
        logger.error("Erro FTP ao baixar %s/%s: %s", pasta, arquivo, exc)
        if os.path.exists(destino_path):
            os.remove(destino_path)
        return None


def baixar_sinan_dengue(
    anos: list[int],
    destino_dir: str = "./downloads",
) -> list[str]:
    """
    Baixa arquivos SINAN para os anos informados.

    Otimizacao: listagem de cada pasta e cacheada para evitar conexoes repetidas.
    """
    baixados: list[str] = []
    cache_listagem: dict[str, dict[str, str]] = {}

    def _arquivos_normalizados(pasta: str) -> dict[str, str]:
        if pasta not in cache_listagem:
            arquivos = listar_arquivos(pasta)
            cache_listagem[pasta] = {nome.upper(): nome for nome in arquivos}
        return cache_listagem[pasta]

    for ano in anos:
        aa = str(ano)[2:]
        nome_arquivo = f"DENGBR{aa}.dbc"
        nome_arquivo_upper = nome_arquivo.upper()

        pasta = PASTAS_FTP["SINAN_FINAL"]
        disponiveis = _arquivos_normalizados(pasta)
        if nome_arquivo_upper in disponiveis:
            caminho = baixar_arquivo(pasta, disponiveis[nome_arquivo_upper], destino_dir)
            if caminho:
                baixados.append(caminho)
                continue

        logger.info("%s nao encontrado em FINAIS, tentando PRELIM...", nome_arquivo)
        pasta = PASTAS_FTP["SINAN_PRELIM"]
        disponiveis = _arquivos_normalizados(pasta)
        if nome_arquivo_upper in disponiveis:
            caminho = baixar_arquivo(pasta, disponiveis[nome_arquivo_upper], destino_dir)
            if caminho:
                baixados.append(caminho)
        else:
            logger.warning("Arquivo %s nao encontrado em FINAIS nem PRELIM", nome_arquivo)

    return baixados


def _competencia_anterior(competencia: str) -> str:
    aa = int(competencia[:2])
    mm = int(competencia[2:])
    if mm == 1:
        aa = (aa - 1) % 100
        mm = 12
    else:
        mm -= 1
    return f"{aa:02d}{mm:02d}"


def baixar_cnes(
    uf: str,
    competencia: str,
    tipos: list[str] | None = None,
    destino_dir: str = "./downloads",
) -> dict[str, str]:
    if tipos is None:
        tipos = ["ST", "LT", "SR"]

    resultado: dict[str, str] = {}

    for tipo in tipos:
        chave_pasta = f"CNES_{tipo}"
        if chave_pasta not in PASTAS_FTP:
            logger.warning("Tipo CNES desconhecido: %s", tipo)
            continue

        pasta = PASTAS_FTP[chave_pasta]
        disponiveis = listar_arquivos(pasta)
        nomes_upper = {a.upper(): a for a in disponiveis}

        arquivo = f"{tipo}{uf.upper()}{competencia}.dbc"
        arquivo_upper = arquivo.upper()
        if arquivo_upper in nomes_upper:
            caminho = baixar_arquivo(pasta, nomes_upper[arquivo_upper], destino_dir)
            if caminho:
                resultado[tipo] = caminho
                continue

        comp_ant = _competencia_anterior(competencia)
        arquivo_ant = f"{tipo}{uf.upper()}{comp_ant}.dbc"
        arquivo_ant_upper = arquivo_ant.upper()
        logger.info(
            "%s nao encontrado, tentando competencia anterior: %s",
            arquivo,
            arquivo_ant,
        )
        if arquivo_ant_upper in nomes_upper:
            caminho = baixar_arquivo(pasta, nomes_upper[arquivo_ant_upper], destino_dir)
            if caminho:
                resultado[tipo] = caminho
        else:
            logger.warning(
                "Arquivo %s (nem %s) encontrado no FTP para tipo %s",
                arquivo,
                arquivo_ant,
                tipo,
            )

    return resultado
