"""
validar_apis.py
Script de validação das fontes de dados externas usadas pelo VígiSUS.
Confirma acessibilidade de todas as APIs e serviços FTP antes de rodar
o pipeline completo.

Uso:
    python validar_apis.py
"""

import sys
import urllib3

# Garante que o diretório pai (data-pipeline/) está no path
import os
sys.path.insert(0, os.path.dirname(__file__))

# Suprimir avisos de certificado SSL não verificado
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

from clients import ibge_client, openmeteo_client, opendatasus_client, datasus_ftp_client

_CO_IBGE_LAVRAS = "3131307"
_LAT_LAVRAS = -21.245
_LON_LAVRAS = -44.999


def _check(numero: int, descricao: str, fn) -> bool:
    print(f"\n[{numero}/7] {descricao}")
    try:
        resultado = fn()
        if resultado is not None and resultado is not False and resultado != [] and resultado != {}:
            print(f"  [OK] {resultado}")
            return True
        print("  [ERRO] Retorno vazio ou None")
        return False
    except Exception as exc:
        print(f"  [ERRO] {exc}")
        return False


# ── Teste 1 ─────────────────────────────────────────────────────────────────
def _teste_ibge_municipio():
    dado = ibge_client.get_municipio(_CO_IBGE_LAVRAS)
    if dado:
        return f"nome={dado['no_municipio']}, uf={dado['sg_uf']}"
    return None


# ── Teste 2 ─────────────────────────────────────────────────────────────────
def _teste_ibge_populacao():
    pop = ibge_client.get_populacao(_CO_IBGE_LAVRAS, 2023)
    if pop is not None:
        return f"população={pop:,}"
    if ibge_client.sidra_disponivel(2023):
        return "endpoint SIDRA acessível (sem dado para o município/ano informado)"
    return None


# ── Teste 3 ─────────────────────────────────────────────────────────────────
def _teste_openmeteo_atual():
    clima = openmeteo_client.get_clima_atual(_LAT_LAVRAS, _LON_LAVRAS)
    if clima:
        return f"temperatura={clima['temperatura']}°C, umidade={clima['umidade']}%"
    return None


# ── Teste 4 ─────────────────────────────────────────────────────────────────
def _teste_openmeteo_previsao():
    previsao = openmeteo_client.get_previsao_16_dias(_LAT_LAVRAS, _LON_LAVRAS)
    if previsao:
        primeiro = previsao[0]
        return f"data={primeiro['data']}, temp_max={primeiro['temp_max']}°C, chuva={primeiro['chuva_mm']}mm"
    return None


# ── Teste 5 ─────────────────────────────────────────────────────────────────
def _teste_opendatasus():
    datasets = opendatasus_client.listar_datasets("dengue")
    if datasets:
        return f"título={datasets[0]['titulo']}"
    return None


# ── Teste 6 ─────────────────────────────────────────────────────────────────
def _teste_ftp_sinan():
    pasta = datasus_ftp_client.PASTAS_FTP["SINAN_FINAL"]
    arquivos = datasus_ftp_client.listar_arquivos(pasta)
    deng = [a for a in arquivos if "DENG" in a.upper()]
    if deng:
        return f"arquivos DENG disponíveis: {deng[:5]}"
    if arquivos:
        return f"arquivos disponíveis (sem DENG): {arquivos[:5]}"
    return None


# ── Teste 7 ─────────────────────────────────────────────────────────────────
def _teste_ftp_cnes():
    pasta = datasus_ftp_client.PASTAS_FTP["CNES_LT"]
    arquivos = datasus_ftp_client.listar_arquivos(pasta)
    mg = [a for a in arquivos if "MG" in a.upper()]
    if mg:
        return f"arquivos MG disponíveis: {mg[:5]}"
    if arquivos:
        return f"arquivos disponíveis (sem MG): {arquivos[:5]}"
    return None


# ── Main ─────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    print("=" * 60)
    print("VígiSUS — Validação de fontes de dados externas")
    print("=" * 60)

    testes = [
        (1, "IBGE Localidades → municipio 3131307 (Lavras)", _teste_ibge_municipio),
        (2, "IBGE SIDRA — População → Lavras 2023", _teste_ibge_populacao),
        (3, "Open-Meteo — Clima atual → (-21.245, -44.999)", _teste_openmeteo_atual),
        (4, "Open-Meteo — Previsão 16 dias", _teste_openmeteo_previsao),
        (5, "OpenDataSUS CKAN → datasets dengue", _teste_opendatasus),
        (6, "FTP DATASUS SINAN → lista /SINAN/DADOS/FINAIS/", _teste_ftp_sinan),
        (7, "FTP DATASUS CNES → lista /CNES/.../Dados/LT/", _teste_ftp_cnes),
    ]

    resultados = [_check(num, desc, fn) for num, desc, fn in testes]
    ok = sum(resultados)
    total = len(testes)
    erros = total - ok

    print("\n" + "=" * 60)
    if erros == 0:
        print(f"APIs OK: {ok}/{total} — Execute python run_all.py")
    else:
        print(f"ATENÇÃO: {erros} fontes com erro — verifique conexão")
    print("=" * 60)

    sys.exit(0 if erros == 0 else 1)
