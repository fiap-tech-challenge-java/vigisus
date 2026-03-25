"""
seed_mock.py
Insere dados mockados com estrutura real para uso em desenvolvimento local,
quando o FTP do DATASUS não estiver acessível.

Dados inseridos:
  - 22 municípios (MG + SP)
  - 1 hospital por cidade + 3 originais de Lavras
  - Leitos e serviços por hospital
  - Casos de dengue semanais para todos os municípios, anos 2021-2026
    (2025: ano completo com cenários distintos por município;
     2026: dados parciais até semana ≈12, simulando ano em curso)
"""

import logging
import math
import random
import sys

from sqlalchemy import create_engine, text, exc

from config import DB_URL

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Dados de municípios — 22 cidades (MG + SP)
# ---------------------------------------------------------------------------
MUNICIPIOS = [
    # Minas Gerais
    {"id": 3138203, "co_ibge": "3138203", "no_municipio": "Lavras",               "sg_uf": "MG", "nu_latitude": -21.2453, "nu_longitude": -44.9994, "populacao": 105000},
    {"id": 3106200, "co_ibge": "3106200", "no_municipio": "Belo Horizonte",       "sg_uf": "MG", "nu_latitude": -19.9191, "nu_longitude": -43.9386, "populacao": 2722000},
    {"id": 3170206, "co_ibge": "3170206", "no_municipio": "Uberlândia",           "sg_uf": "MG", "nu_latitude": -18.9186, "nu_longitude": -48.2772, "populacao": 706597},
    {"id": 3136702, "co_ibge": "3136702", "no_municipio": "Juiz de Fora",         "sg_uf": "MG", "nu_latitude": -21.7642, "nu_longitude": -43.3503, "populacao": 577000},
    {"id": 3143302, "co_ibge": "3143302", "no_municipio": "Montes Claros",        "sg_uf": "MG", "nu_latitude": -16.7282, "nu_longitude": -43.8617, "populacao": 414649},
    {"id": 3152501, "co_ibge": "3152501", "no_municipio": "Pouso Alegre",         "sg_uf": "MG", "nu_latitude": -22.2289, "nu_longitude": -45.9373, "populacao": 154116},
    {"id": 3170107, "co_ibge": "3170107", "no_municipio": "Uberaba",              "sg_uf": "MG", "nu_latitude": -19.7483, "nu_longitude": -47.9317, "populacao": 338000},
    {"id": 3118601, "co_ibge": "3118601", "no_municipio": "Contagem",             "sg_uf": "MG", "nu_latitude": -19.9319, "nu_longitude": -44.0536, "populacao": 668000},
    {"id": 3147907, "co_ibge": "3147907", "no_municipio": "Passos",               "sg_uf": "MG", "nu_latitude": -20.7189, "nu_longitude": -46.6100, "populacao": 110000},
    {"id": 3127701, "co_ibge": "3127701", "no_municipio": "Governador Valadares", "sg_uf": "MG", "nu_latitude": -18.8514, "nu_longitude": -41.9494, "populacao": 281000},
    {"id": 3154606, "co_ibge": "3154606", "no_municipio": "Ribeirão das Neves",   "sg_uf": "MG", "nu_latitude": -19.7706, "nu_longitude": -44.0858, "populacao": 340000},
    {"id": 3170701, "co_ibge": "3170701", "no_municipio": "Varginha",             "sg_uf": "MG", "nu_latitude": -21.5512, "nu_longitude": -45.4302, "populacao": 135000},
    {"id": 3131307, "co_ibge": "3131307", "no_municipio": "Ipatinga",             "sg_uf": "MG", "nu_latitude": -19.4683, "nu_longitude": -42.5364, "populacao": 262000},
    {"id": 3119401, "co_ibge": "3119401", "no_municipio": "Coronel Fabriciano",   "sg_uf": "MG", "nu_latitude": -19.5186, "nu_longitude": -42.6347, "populacao": 115000},
    # São Paulo
    {"id": 3550308, "co_ibge": "3550308", "no_municipio": "São Paulo",            "sg_uf": "SP", "nu_latitude": -23.5505, "nu_longitude": -46.6333, "populacao": 12325000},
    {"id": 3509502, "co_ibge": "3509502", "no_municipio": "Campinas",             "sg_uf": "SP", "nu_latitude": -22.9056, "nu_longitude": -47.0608, "populacao": 1213000},
    {"id": 3543402, "co_ibge": "3543402", "no_municipio": "Ribeirão Preto",       "sg_uf": "SP", "nu_latitude": -21.1775, "nu_longitude": -47.8103, "populacao": 718000},
    {"id": 3534401, "co_ibge": "3534401", "no_municipio": "Osasco",               "sg_uf": "SP", "nu_latitude": -23.5329, "nu_longitude": -46.7920, "populacao": 696000},
    {"id": 3518800, "co_ibge": "3518800", "no_municipio": "Guarulhos",            "sg_uf": "SP", "nu_latitude": -23.4543, "nu_longitude": -46.5333, "populacao": 1392000},
    {"id": 3538709, "co_ibge": "3538709", "no_municipio": "Piracicaba",           "sg_uf": "SP", "nu_latitude": -22.7253, "nu_longitude": -47.6492, "populacao": 412000},
    {"id": 3529005, "co_ibge": "3529005", "no_municipio": "Marília",              "sg_uf": "SP", "nu_latitude": -22.2139, "nu_longitude": -49.9458, "populacao": 240000},
    {"id": 3552205, "co_ibge": "3552205", "no_municipio": "Sorocaba",             "sg_uf": "SP", "nu_latitude": -23.5015, "nu_longitude": -47.4526, "populacao": 688000},
]

# ---------------------------------------------------------------------------
# Estabelecimentos — 3 originais de Lavras + 1 por cidade nova
# ---------------------------------------------------------------------------

# Hospitais grandes: BH, SP, Campinas
_GRANDES = {"3106200", "3550308", "3509502"}
# Hospitais médios: demais capitais e cidades > ~300k
_MEDIOS = {"3170206", "3136702", "3143302",
           "3154606", "3543402", "3534401", "3518800", "3538709", "3552205",
           "3131307", "3170107", "3118601", "3147907", "3127701", "3170701",
           "3119401", "3529005"}

ESTABELECIMENTOS = [
    # Originais de Lavras (sem campo id — SERIAL gera)
    {"co_cnes": "2078023", "no_fantasia": "Hospital Universitário UFLA",  "co_municipio": "3138203", "nu_latitude": -21.2430, "nu_longitude": -44.9870, "nu_telefone": "3514050000", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "2078015", "no_fantasia": "Santa Casa de Lavras",          "co_municipio": "3138203", "nu_latitude": -21.2489, "nu_longitude": -45.0010, "nu_telefone": "3514060000", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "2000000", "no_fantasia": "UPA Lavras Norte",               "co_municipio": "3138203", "nu_latitude": -21.2300, "nu_longitude": -44.9900, "nu_telefone": "3514070000", "tp_gestao": "M", "competencia": "202312"},
    # 1 hospital por cidade nova
    {"co_cnes": "9001001", "no_fantasia": "Hospital Regional BH",           "co_municipio": "3106200", "nu_latitude": -19.9191, "nu_longitude": -43.9386, "nu_telefone": "3100000001", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9003001", "no_fantasia": "Hospital Metropolitano BH",      "co_municipio": "3106200", "nu_latitude": -19.9130, "nu_longitude": -43.9400, "nu_telefone": "3133333001", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9003002", "no_fantasia": "Hospital Central BH",            "co_municipio": "3106200", "nu_latitude": -19.9270, "nu_longitude": -43.9340, "nu_telefone": "3133333002", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9001002", "no_fantasia": "Hospital Regional Uberlândia",   "co_municipio": "3170206", "nu_latitude": -18.9186, "nu_longitude": -48.2772, "nu_telefone": "3400000002", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9001003", "no_fantasia": "Hospital Regional Juiz de Fora", "co_municipio": "3136702", "nu_latitude": -21.7642, "nu_longitude": -43.3503, "nu_telefone": "3200000003", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9001004", "no_fantasia": "Hospital Regional Montes Claros","co_municipio": "3143302", "nu_latitude": -16.7282, "nu_longitude": -43.8617, "nu_telefone": "3800000004", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9001005", "no_fantasia": "Hospital Regional Pouso Alegre", "co_municipio": "3152501", "nu_latitude": -22.2289, "nu_longitude": -45.9373, "nu_telefone": "3500000005", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9001006", "no_fantasia": "Hospital Regional Uberaba",      "co_municipio": "3170107", "nu_latitude": -19.7483, "nu_longitude": -47.9317, "nu_telefone": "3400000006", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9001007", "no_fantasia": "Hospital Regional Contagem",     "co_municipio": "3118601", "nu_latitude": -19.9319, "nu_longitude": -44.0536, "nu_telefone": "3100000007", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9001008", "no_fantasia": "Hospital Regional Passos",       "co_municipio": "3147907", "nu_latitude": -20.7189, "nu_longitude": -46.6100, "nu_telefone": "3500000008", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9001009", "no_fantasia": "Hospital Regional Gov. Valadares","co_municipio": "3127701", "nu_latitude": -18.8514, "nu_longitude": -41.9494, "nu_telefone": "3300000009", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9001010", "no_fantasia": "Hospital Regional Rib. das Neves","co_municipio": "3154606", "nu_latitude": -19.7706, "nu_longitude": -44.0858, "nu_telefone": "3100000010", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9001011", "no_fantasia": "Hospital Regional Varginha",     "co_municipio": "3170701", "nu_latitude": -21.5512, "nu_longitude": -45.4302, "nu_telefone": "3500000011", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9001012", "no_fantasia": "Hospital Regional São Paulo",    "co_municipio": "3550308", "nu_latitude": -23.5505, "nu_longitude": -46.6333, "nu_telefone": "1100000012", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9003003", "no_fantasia": "Hospital Paulista Centro",       "co_municipio": "3550308", "nu_latitude": -23.5450, "nu_longitude": -46.6380, "nu_telefone": "1133333003", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9003004", "no_fantasia": "Hospital Municipal SP Leste",    "co_municipio": "3550308", "nu_latitude": -23.5590, "nu_longitude": -46.6200, "nu_telefone": "1133333004", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9001013", "no_fantasia": "Hospital Regional Campinas",     "co_municipio": "3509502", "nu_latitude": -22.9056, "nu_longitude": -47.0608, "nu_telefone": "1900000013", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9001014", "no_fantasia": "Hospital Regional Ribeirão Preto","co_municipio": "3543402", "nu_latitude": -21.1775, "nu_longitude": -47.8103, "nu_telefone": "1600000014", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9001015", "no_fantasia": "Hospital Regional Osasco",       "co_municipio": "3534401", "nu_latitude": -23.5329, "nu_longitude": -46.7920, "nu_telefone": "1100000015", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9001016", "no_fantasia": "Hospital Regional Guarulhos",    "co_municipio": "3518800", "nu_latitude": -23.4543, "nu_longitude": -46.5333, "nu_telefone": "1100000016", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9001017", "no_fantasia": "Hospital Regional Piracicaba",   "co_municipio": "3538709", "nu_latitude": -22.7253, "nu_longitude": -47.6492, "nu_telefone": "1900000017", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9001018", "no_fantasia": "Hospital Regional Marília",      "co_municipio": "3529005", "nu_latitude": -22.2139, "nu_longitude": -49.9458, "nu_telefone": "1400000018", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9001019", "no_fantasia": "Hospital Regional Sorocaba",     "co_municipio": "3552205", "nu_latitude": -23.5015, "nu_longitude": -47.4526, "nu_telefone": "1500000019", "tp_gestao": "E", "competencia": "202312"},
    {"co_cnes": "9002001", "no_fantasia": "Hospital Municipal de Ipatinga", "co_municipio": "3131307", "nu_latitude": -19.4700, "nu_longitude": -42.5400, "nu_telefone": "3138291000", "tp_gestao": "M", "competencia": "202312"},
    {"co_cnes": "9002002", "no_fantasia": "Hospital Municipal Cel. Fabriciano", "co_municipio": "3119401", "nu_latitude": -19.5190, "nu_longitude": -42.6350, "nu_telefone": "3138261000", "tp_gestao": "M", "competencia": "202312"},
]

# ---------------------------------------------------------------------------
# Leitos — por hospital, de acordo com porte
# ---------------------------------------------------------------------------

def _leito_counts(co_cnes: str, co_municipio: str) -> list:
    """Retorna lista de dicts de leitos para o hospital, sem campo id."""
    if co_municipio in _GRANDES:
        qt_clinico, qt_uti, qt_cirurgico = 200, 80, 120
    elif co_municipio in _MEDIOS:
        qt_clinico, qt_uti, qt_cirurgico = 80, 30, 40
    else:
        # Lavras, Passos, Pouso Alegre, Varginha
        qt_clinico, qt_uti, qt_cirurgico = 40, 10, 20

    sus_ratio = 0.9
    return [
        {"co_cnes": co_cnes, "tp_leito": "74", "ds_leito": "CLÍNICO",       "qt_exist": qt_clinico,   "qt_sus": math.floor(qt_clinico * sus_ratio),   "competencia": "202312"},
        {"co_cnes": co_cnes, "tp_leito": "81", "ds_leito": "UTI ADULTO II", "qt_exist": qt_uti,       "qt_sus": math.floor(qt_uti * sus_ratio),       "competencia": "202312"},
        {"co_cnes": co_cnes, "tp_leito": "76", "ds_leito": "CIRÚRGICO",     "qt_exist": qt_cirurgico, "qt_sus": math.floor(qt_cirurgico * sus_ratio), "competencia": "202312"},
    ]


# Map co_cnes → co_municipio for easy lookup
_CNES_TO_MUN = {e["co_cnes"]: e["co_municipio"] for e in ESTABELECIMENTOS}

LEITOS = []
for _est in ESTABELECIMENTOS:
    LEITOS.extend(_leito_counts(_est["co_cnes"], _est["co_municipio"]))

# ---------------------------------------------------------------------------
# Serviços especializados — sem campo id
# ---------------------------------------------------------------------------

SERVICOS = []
for _est in ESTABELECIMENTOS:
    co = _est["co_cnes"]
    mun = _est["co_municipio"]
    # Doenças infecciosas para todos
    SERVICOS.append({"co_cnes": co, "serv_esp": "135", "class_sr": "001", "competencia": "202312"})
    # Infectologia para hospitais grandes e médios
    if mun in _GRANDES or mun in _MEDIOS:
        SERVICOS.append({"co_cnes": co, "serv_esp": "136", "class_sr": "001", "competencia": "202312"})

# ---------------------------------------------------------------------------
# Casos de dengue — 22 municípios, anos 2021-2026, sazonalidade realista
# ---------------------------------------------------------------------------

BASE_CASOS = {
    "3138203": 15,   # Lavras
    "3106200": 200,  # Belo Horizonte
    "3170206": 80,   # Uberlândia
    "3136702": 60,   # Juiz de Fora
    "3143302": 45,   # Montes Claros
    "3152501": 18,   # Pouso Alegre
    "3170107": 40,   # Uberaba
    "3118601": 75,   # Contagem
    "3147907": 12,   # Passos
    "3127701": 35,   # Gov. Valadares
    "3154606": 40,   # Ribeirão das Neves
    "3170701": 18,   # Varginha
    "3131307": 35,   # Ipatinga
    "3119401": 12,   # Coronel Fabriciano
    "3550308": 60,   # São Paulo (reduzido para contraste)
    "3509502": 18,   # Campinas
    "3543402": 10,   # Ribeirão Preto
    "3534401": 8,    # Osasco
    "3518800": 14,   # Guarulhos
    "3538709": 6,    # Piracicaba
    "3529005": 3,    # Marília
    "3552205": 8,    # Sorocaba
}

MULT_ANO = {2021: 0.8, 2022: 1.0, 2023: 1.2, 2024: 2.5}

random.seed(42)

CASOS_DENGUE = []
for _co_municipio, _base in BASE_CASOS.items():
    for _ano, _mult in MULT_ANO.items():
        for _semana in range(1, 53):
            if _semana <= 15 or _semana >= 45:
                _casos = int(_base * _mult * random.randint(2, 5))
            else:
                _casos = int(_base * _mult * random.randint(0, 1) / 4)
            CASOS_DENGUE.append(
                {
                    "co_municipio": _co_municipio,
                    "ano": _ano,
                    "semana_epi": _semana,
                    "total_casos": _casos,
                }
            )

# ---------------------------------------------------------------------------
# 2025 — ano completo (52 semanas) com cenários distintos por município
# Multiplicadores: EPIDEMIA ≈ 5–8×, ALTO ≈ 2.5×, MODERADO ≈ 1.2×, BAIXO ≈ 0.5×
# ---------------------------------------------------------------------------
_MULT_2025 = {
    "3138203": 6.25,  # Lavras: EPIDEMIA (2.5× sobre 2024, que já era 2.5×)
    "3106200": 2.5,   # Belo Horizonte: ALTO
    "3131307": 8.0,   # Ipatinga: EPIDEMIA (casos mais altos que Lavras)
    "3170701": 1.2,   # Varginha: MODERADO
    # SP com baixa circulação para gerar contraste de cores (amarelo/verde)
    "3550308": 0.25,  # São Paulo: BAIXO
    "3509502": 0.20,  # Campinas: BAIXO
    "3543402": 0.10,  # Ribeirão Preto: muito baixo
    "3534401": 0.08,  # Osasco: muito baixo
    "3518800": 0.18,  # Guarulhos: baixo
    "3538709": 0.08,  # Piracicaba: muito baixo
    "3529005": 0.00,  # Marília: sem casos em 2025
    "3552205": 0.08,  # Sorocaba: muito baixo
}
_MULT_2025_DEFAULT = 1.8

for _co_municipio, _base in BASE_CASOS.items():
    _mult = _MULT_2025.get(_co_municipio, _MULT_2025_DEFAULT)
    for _semana in range(1, 53):
        if _semana <= 15 or _semana >= 45:
            _casos = int(_base * _mult * random.randint(2, 5))
        else:
            _casos = int(_base * _mult * random.randint(0, 1) / 4)
        CASOS_DENGUE.append(
            {
                "co_municipio": _co_municipio,
                "ano": 2025,
                "semana_epi": _semana,
                "total_casos": _casos,
            }
        )

# ---------------------------------------------------------------------------
# 2026 — ano em curso; dados apenas para semanas 1 até ≈12 (março/2026)
# Todos os municípios do mock recebem dados (com cenários específicos para alguns)
# ---------------------------------------------------------------------------
_SEMANA_ATUAL_2026 = 12

_MULT_2026 = {
    "3138203": 3.0,   # Lavras: ALTO
    "3106200": 1.0,   # Belo Horizonte: MODERADO
    "3131307": 7.0,   # Ipatinga: EPIDEMIA (ver _SEMANA_MAX_2026 para limite de semanas)
    "3170701": 0.5,   # Varginha: BAIXO
    # SP no ano em curso com cenários de pouco/nenhum caso
    "3550308": 0.18,  # São Paulo: BAIXO
    "3509502": 0.15,  # Campinas: BAIXO
    "3543402": 0.08,  # Ribeirão Preto: muito baixo
    "3534401": 0.05,  # Osasco: muito baixo
    "3518800": 0.12,  # Guarulhos: baixo
    "3538709": 0.05,  # Piracicaba: muito baixo
    "3529005": 0.00,  # Marília: sem casos em 2026
    "3552205": 0.05,  # Sorocaba: muito baixo
}
_MULT_2026_DEFAULT = 1.6
_SEMANA_MAX_2026 = {
    "3131307": 10,    # Ipatinga: dados disponíveis apenas até semana 10 (EPIDEMIA em curso)
}

for _co_municipio, _base in BASE_CASOS.items():
    _mult = _MULT_2026.get(_co_municipio, _MULT_2026_DEFAULT)
    _max_s = _SEMANA_MAX_2026.get(_co_municipio, _SEMANA_ATUAL_2026)
    for _semana in range(1, _max_s + 1):
        # Semanas 1-12 estão todas no período de pico (≤ 15)
        _casos = int(_base * _mult * random.randint(2, 5))
        CASOS_DENGUE.append(
            {
                "co_municipio": _co_municipio,
                "ano": 2026,
                "semana_epi": _semana,
                "total_casos": _casos,
            }
        )


# ---------------------------------------------------------------------------
# Funções de inserção
# ---------------------------------------------------------------------------

def seed_municipios(conn) -> int:
    sql = text(
        """
        INSERT INTO municipios
            (id, co_ibge, no_municipio, sg_uf, nu_latitude, nu_longitude, populacao)
        VALUES
            (:id, :co_ibge, :no_municipio, :sg_uf, :nu_latitude, :nu_longitude, :populacao)
        ON CONFLICT (co_ibge) DO UPDATE
            SET no_municipio = EXCLUDED.no_municipio,
                sg_uf        = EXCLUDED.sg_uf,
                nu_latitude  = EXCLUDED.nu_latitude,
                nu_longitude = EXCLUDED.nu_longitude,
                populacao    = EXCLUDED.populacao,
                id           = EXCLUDED.id
        """
    )
    conn.execute(sql, MUNICIPIOS)
    return len(MUNICIPIOS)


def seed_estabelecimentos(conn) -> int:
    sql = text(
        """
        INSERT INTO estabelecimentos
            (co_cnes, no_fantasia, co_municipio,
             nu_latitude, nu_longitude, nu_telefone, tp_gestao, competencia)
        VALUES
            (:co_cnes, :no_fantasia, :co_municipio,
             :nu_latitude, :nu_longitude, :nu_telefone, :tp_gestao, :competencia)
        ON CONFLICT (co_cnes) DO UPDATE
            SET no_fantasia  = EXCLUDED.no_fantasia,
                co_municipio = EXCLUDED.co_municipio,
                nu_latitude  = EXCLUDED.nu_latitude,
                nu_longitude = EXCLUDED.nu_longitude,
                nu_telefone  = EXCLUDED.nu_telefone,
                tp_gestao    = EXCLUDED.tp_gestao
        """
    )
    conn.execute(sql, ESTABELECIMENTOS)
    return len(ESTABELECIMENTOS)


def seed_leitos(conn) -> int:
    sql = text(
        """
        INSERT INTO leitos (co_cnes, tp_leito, ds_leito, qt_exist, qt_sus, competencia)
        VALUES (:co_cnes, :tp_leito, :ds_leito, :qt_exist, :qt_sus, :competencia)
        ON CONFLICT DO NOTHING
        """
    )
    conn.execute(sql, LEITOS)
    return len(LEITOS)


def seed_servicos(conn) -> int:
    sql = text(
        """
        INSERT INTO servicos_especializados (co_cnes, serv_esp, class_sr, competencia)
        VALUES (:co_cnes, :serv_esp, :class_sr, :competencia)
        ON CONFLICT DO NOTHING
        """
    )
    conn.execute(sql, SERVICOS)
    return len(SERVICOS)


def seed_casos_dengue(conn) -> int:
    sql = text(
        """
        INSERT INTO casos_dengue
            (co_municipio, ano, semana_epi, total_casos)
        VALUES
            (:co_municipio, :ano, :semana_epi, :total_casos)
        ON CONFLICT (co_municipio, ano, semana_epi) DO UPDATE
            SET total_casos = EXCLUDED.total_casos
        """
    )
    conn.execute(sql, CASOS_DENGUE)
    return len(CASOS_DENGUE)


def reset_mock_data(conn) -> None:
    """
    Limpa dados mock de MG/SP antes de inserir novamente.
    Isso evita dados antigos/duplicados (ex.: códigos IBGE antigos) impactarem buscas.
    """
    conn.execute(
        text(
            """
            DELETE FROM casos_dengue
            WHERE co_municipio IN (
                SELECT co_ibge FROM municipios WHERE sg_uf IN ('MG', 'SP')
            )
            """
        )
    )

    conn.execute(
        text(
            """
            DELETE FROM servicos_especializados
            WHERE co_cnes IN (
                SELECT co_cnes FROM estabelecimentos
                WHERE co_municipio IN (
                    SELECT co_ibge FROM municipios WHERE sg_uf IN ('MG', 'SP')
                )
            )
            """
        )
    )

    conn.execute(
        text(
            """
            DELETE FROM leitos
            WHERE co_cnes IN (
                SELECT co_cnes FROM estabelecimentos
                WHERE co_municipio IN (
                    SELECT co_ibge FROM municipios WHERE sg_uf IN ('MG', 'SP')
                )
            )
            """
        )
    )

    conn.execute(
        text(
            """
            DELETE FROM estabelecimentos
            WHERE co_municipio IN (
                SELECT co_ibge FROM municipios WHERE sg_uf IN ('MG', 'SP')
            )
            """
        )
    )

    conn.execute(
        text(
            """
            DELETE FROM municipios
            WHERE sg_uf IN ('MG', 'SP')
            """
        )
    )


def run() -> None:
    # Verificar conexão com banco de dados
    try:
        engine = create_engine(DB_URL)
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
    except exc.OperationalError as e:
        logger.error("❌ Erro: Não foi possível conectar ao banco de dados PostgreSQL.")
        logger.error("")
        logger.error("Por favor, inicie o Docker com o comando:")
        logger.error("")
        logger.error("  docker-compose up -d postgres")
        logger.error("")
        logger.error("E aguarde alguns segundos para o banco de dados ficar pronto.")
        logger.error("")
        logger.error("Detalhes do erro: %s", str(e).split('\n')[0] if '\n' in str(e) else str(e))
        sys.exit(1)
    
    engine = create_engine(DB_URL)
    with engine.begin() as conn:
        reset_mock_data(conn)
        logger.info("Dados mock antigos de MG/SP removidos")

        n_mun = seed_municipios(conn)
        logger.info("Inseridos %d municípios mock", n_mun)

        n_est = seed_estabelecimentos(conn)
        logger.info("Inseridos %d estabelecimentos mock", n_est)

        n_lei = seed_leitos(conn)
        logger.info("Inseridos %d leitos mock", n_lei)

        n_srv = seed_servicos(conn)
        logger.info("Inseridos %d serviços mock", n_srv)

        n_cas = seed_casos_dengue(conn)
        logger.info("Inseridos %d casos de dengue mock", n_cas)

    logger.info(
        "Seed concluído: %d municípios, %d hospitais, %d leitos, "
        "%d serviços, %d registros de dengue",
        n_mun,
        n_est,
        n_lei,
        n_srv,
        n_cas,
    )


if __name__ == "__main__":
    run()
