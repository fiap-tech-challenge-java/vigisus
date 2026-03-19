"""
clients/datasus_ftp_client.py
Cliente FTP para download de arquivos públicos do DATASUS.
Acessa o servidor ftp.datasus.gov.br com login anônimo (sem senha).
"""

import ftplib
import logging
import os

logger = logging.getLogger(__name__)

_FTP_HOST = "ftp.datasus.gov.br"
_FTP_TIMEOUT = 60

PASTAS_FTP = {
    "SINAN_FINAL":  "/dissemin/publicos/SINAN/DADOS/FINAIS/",
    "SINAN_PRELIM": "/dissemin/publicos/SINAN/DADOS/PRELIM/",
    "CNES_ST":      "/dissemin/publicos/CNES/200508_/Dados/ST/",
    "CNES_LT":      "/dissemin/publicos/CNES/200508_/Dados/LT/",
    "CNES_SR":      "/dissemin/publicos/CNES/200508_/Dados/SR/",
}


def _connect() -> ftplib.FTP:
    """Abre conexão FTP anônima com o servidor DATASUS."""
    ftp = ftplib.FTP(timeout=_FTP_TIMEOUT)
    ftp.connect(_FTP_HOST)
    ftp.login()  # anônimo, sem senha
    return ftp


def listar_arquivos(pasta: str) -> list[str]:
    """
    Lista os arquivos disponíveis em uma pasta do FTP DATASUS.

    :param pasta: Caminho remoto (ex.: PASTAS_FTP['SINAN_FINAL']).
    :return: Lista de nomes de arquivo.
    """
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
    """
    Baixa um arquivo do FTP DATASUS para o diretório local.

    Pula o download se o arquivo já existir localmente.
    Exibe progresso a cada MB baixado.

    :param pasta: Caminho remoto da pasta.
    :param arquivo: Nome do arquivo no FTP.
    :param destino_dir: Diretório local de destino.
    :return: Caminho completo do arquivo baixado ou None em caso de erro.
    """
    os.makedirs(destino_dir, exist_ok=True)
    destino_path = os.path.join(destino_dir, arquivo)

    if os.path.exists(destino_path):
        logger.info("Arquivo já existe, pulando: %s", destino_path)
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
            "Download concluído: %s (%.2f MB)",
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
    Baixa os arquivos SINAN de dengue para os anos informados.

    Tenta primeiro a pasta de arquivos finais e, se não encontrar, tenta
    a pasta de arquivos preliminares.

    :param anos: Lista de anos (ex.: [2022, 2023, 2024]).
    :param destino_dir: Diretório local de destino.
    :return: Lista de caminhos dos arquivos baixados com sucesso.
    """
    baixados = []

    for ano in anos:
        aa = str(ano)[2:]  # ex.: 2024 → "24"
        nome_arquivo = f"DENGBR{aa}.dbc"

        # Tenta pasta FINAIS
        pasta = PASTAS_FTP["SINAN_FINAL"]
        disponiveis = listar_arquivos(pasta)
        if nome_arquivo in disponiveis or nome_arquivo.upper() in [a.upper() for a in disponiveis]:
            caminho = baixar_arquivo(pasta, nome_arquivo, destino_dir)
            if caminho:
                baixados.append(caminho)
                continue

        # Fallback: pasta PRELIM
        logger.info("%s não encontrado em FINAIS, tentando PRELIM...", nome_arquivo)
        pasta = PASTAS_FTP["SINAN_PRELIM"]
        disponiveis = listar_arquivos(pasta)
        if nome_arquivo in disponiveis or nome_arquivo.upper() in [a.upper() for a in disponiveis]:
            caminho = baixar_arquivo(pasta, nome_arquivo, destino_dir)
            if caminho:
                baixados.append(caminho)
        else:
            logger.warning("Arquivo %s não encontrado em FINAIS nem PRELIM", nome_arquivo)

    return baixados


def _competencia_anterior(competencia: str) -> str:
    """
    Retorna a competência anterior no formato AAMM.

    Ex.: '2501' → '2412', '2503' → '2502'.
    """
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
    tipos: list[str] = None,
    destino_dir: str = "./downloads",
) -> dict[str, str]:
    """
    Baixa arquivos CNES do FTP para uma UF e competência.

    Se o arquivo da competência informada não existir no FTP,
    tenta automaticamente a competência anterior (AAMM - 1).

    :param uf: Sigla da UF (ex.: 'MG').
    :param competencia: Competência no formato AAMM (ex.: '2502').
    :param tipos: Lista de tipos CNES (padrão ['ST', 'LT', 'SR']).
    :param destino_dir: Diretório local de destino.
    :return: Dicionário { tipo: caminho_local }.
    """
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
        nomes_upper = [a.upper() for a in disponiveis]

        # Tenta competência informada
        arquivo = f"{tipo}{uf.upper()}{competencia}.dbc"
        if arquivo.upper() in nomes_upper:
            caminho = baixar_arquivo(pasta, arquivo, destino_dir)
            if caminho:
                resultado[tipo] = caminho
                continue

        # Tenta competência anterior
        comp_ant = _competencia_anterior(competencia)
        arquivo_ant = f"{tipo}{uf.upper()}{comp_ant}.dbc"
        logger.info(
            "%s não encontrado, tentando competência anterior: %s",
            arquivo,
            arquivo_ant,
        )
        if arquivo_ant.upper() in nomes_upper:
            caminho = baixar_arquivo(pasta, arquivo_ant, destino_dir)
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
