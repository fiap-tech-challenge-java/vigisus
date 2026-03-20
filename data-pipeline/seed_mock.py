"""
seed_mock.py
Insere dados mockados com estrutura real para uso em desenvolvimento local,
quando o FTP do DATASUS não estiver acessível.

Dados inseridos:
  - 5 municípios de MG (incluindo Lavras cod 3131307)
  - 3 hospitais com leitos e serviços
  - 2 anos de casos de dengue semanais para Lavras (semanas 1-52)
"""

import logging
import random

from sqlalchemy import create_engine, text

from config import DB_URL

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Dados de municípios de MG
# ---------------------------------------------------------------------------
MUNICIPIOS = [
    {
        "id": 3131307,
        "co_ibge": "3131307",
        "no_municipio": "Lavras",
        "sg_uf": "MG",
        "nu_latitude": -21.2453,
        "nu_longitude": -44.9994,
        "populacao": 105000,
    },
    {
        "id": 3106200,
        "co_ibge": "3106200",
        "no_municipio": "Belo Horizonte",
        "sg_uf": "MG",
        "nu_latitude": -19.9191,
        "nu_longitude": -43.9386,
        "populacao": 2722000,
    },
    {
        "id": 3170206,
        "co_ibge": "3170206",
        "no_municipio": "Uberlândia",
        "sg_uf": "MG",
        "nu_latitude": -18.9186,
        "nu_longitude": -48.2772,
        "populacao": 706597,
    },
    {
        "id": 3143302,
        "co_ibge": "3143302",
        "no_municipio": "Montes Claros",
        "sg_uf": "MG",
        "nu_latitude": -16.7282,
        "nu_longitude": -43.8617,
        "populacao": 414649,
    },
    {
        "id": 3152501,
        "co_ibge": "3152501",
        "no_municipio": "Pouso Alegre",
        "sg_uf": "MG",
        "nu_latitude": -22.2289,
        "nu_longitude": -45.9373,
        "populacao": 154116,
    },
]

# ---------------------------------------------------------------------------
# Estabelecimentos (hospitais)
# ---------------------------------------------------------------------------
ESTABELECIMENTOS = [
    {
        "id": 2078023,
        "co_cnes": "2078023",
        "no_fantasia": "Hospital Universitário UFLA",
        "co_municipio": "3131307",
        "nu_latitude": -21.2430,
        "nu_longitude": -44.9870,
        "nu_telefone": "3514050000",
        "tp_gestao": "M",
    },
    {
        "id": 2078015,
        "co_cnes": "2078015",
        "no_fantasia": "Santa Casa de Lavras",
        "co_municipio": "3131307",
        "nu_latitude": -21.2489,
        "nu_longitude": -45.0010,
        "nu_telefone": "3514060000",
        "tp_gestao": "M",
    },
    {
        "id": 2000000,
        "co_cnes": "2000000",
        "no_fantasia": "UPA Lavras Norte",
        "co_municipio": "3131307",
        "nu_latitude": -21.2300,
        "nu_longitude": -44.9900,
        "nu_telefone": "3514070000",
        "tp_gestao": "M",
    },
]

# ---------------------------------------------------------------------------
# Leitos
# ---------------------------------------------------------------------------
LEITOS = [
    {
        "id": 1,
        "co_cnes": "2078023",
        "tp_leito": "74",
        "ds_leito": "CIRÚRGICO",
        "qt_exist": 30,
        "qt_sus": 25,
    },
    {
        "id": 2,
        "co_cnes": "2078023",
        "tp_leito": "01",
        "ds_leito": "CLÍNICO",
        "qt_exist": 50,
        "qt_sus": 45,
    },
    {
        "id": 3,
        "co_cnes": "2078015",
        "tp_leito": "74",
        "ds_leito": "CIRÚRGICO",
        "qt_exist": 20,
        "qt_sus": 18,
    },
    {
        "id": 4,
        "co_cnes": "2078015",
        "tp_leito": "01",
        "ds_leito": "CLÍNICO",
        "qt_exist": 40,
        "qt_sus": 38,
    },
    {
        "id": 5,
        "co_cnes": "2000000",
        "tp_leito": "02",
        "ds_leito": "URGÊNCIA",
        "qt_exist": 10,
        "qt_sus": 10,
    },
]

# ---------------------------------------------------------------------------
# Serviços especializados
# ---------------------------------------------------------------------------
SERVICOS = [
    {"id": 1, "co_cnes": "2078023", "serv_esp": "135", "class_sr": "001"},
    {"id": 2, "co_cnes": "2078023", "serv_esp": "040", "class_sr": "002"},
    {"id": 3, "co_cnes": "2078015", "serv_esp": "135", "class_sr": "001"},
    {"id": 4, "co_cnes": "2000000", "serv_esp": "118", "class_sr": "001"},
]

# ---------------------------------------------------------------------------
# Casos de dengue — 2 anos semanais para Lavras
# ---------------------------------------------------------------------------

random.seed(42)

CASOS_DENGUE = []
_caso_id = 1
for year in [2023, 2024]:
    for semana in range(1, 53):
        # Simula sazonalidade: pico nas semanas 1-15 e 45-52
        if semana <= 15 or semana >= 45:
            casos = random.randint(20, 120)
        else:
            casos = random.randint(2, 30)
        CASOS_DENGUE.append(
            {
                "id": _caso_id,
                "co_municipio": "3131307",
                "ano": year,
                "semana_epi": semana,
                "total_casos": casos,
            }
        )
        _caso_id += 1


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
            (id, co_cnes, no_fantasia, co_municipio,
             nu_latitude, nu_longitude, nu_telefone, tp_gestao)
        VALUES
            (:id, :co_cnes, :no_fantasia, :co_municipio,
             :nu_latitude, :nu_longitude, :nu_telefone, :tp_gestao)
        ON CONFLICT (co_cnes) DO UPDATE
            SET no_fantasia         = EXCLUDED.no_fantasia,
                co_municipio        = EXCLUDED.co_municipio,
                nu_latitude         = EXCLUDED.nu_latitude,
                nu_longitude        = EXCLUDED.nu_longitude,
                nu_telefone         = EXCLUDED.nu_telefone,
                tp_gestao           = EXCLUDED.tp_gestao,
                id                  = EXCLUDED.id
        """
    )
    conn.execute(sql, ESTABELECIMENTOS)
    return len(ESTABELECIMENTOS)


def seed_leitos(conn) -> int:
    sql = text(
        """
        INSERT INTO leitos (id, co_cnes, tp_leito, ds_leito, qt_exist, qt_sus)
        VALUES (:id, :co_cnes, :tp_leito, :ds_leito, :qt_exist, :qt_sus)
        ON CONFLICT (id) DO UPDATE
            SET co_cnes = EXCLUDED.co_cnes,
                tp_leito = EXCLUDED.tp_leito,
                ds_leito = EXCLUDED.ds_leito,
                qt_exist = EXCLUDED.qt_exist,
                qt_sus   = EXCLUDED.qt_sus
        """
    )
    conn.execute(sql, LEITOS)
    return len(LEITOS)


def seed_servicos(conn) -> int:
    sql = text(
        """
        INSERT INTO servicos_especializados (id, co_cnes, serv_esp, class_sr)
        VALUES (:id, :co_cnes, :serv_esp, :class_sr)
        ON CONFLICT (id) DO UPDATE
            SET co_cnes  = EXCLUDED.co_cnes,
                serv_esp = EXCLUDED.serv_esp,
                class_sr = EXCLUDED.class_sr
        """
    )
    conn.execute(sql, SERVICOS)
    return len(SERVICOS)


def seed_casos_dengue(conn) -> int:
    sql = text(
        """
        INSERT INTO casos_dengue
            (id, co_municipio, ano, semana_epi, total_casos)
        VALUES
            (:id, :co_municipio, :ano, :semana_epi, :total_casos)
        ON CONFLICT (co_municipio, ano, semana_epi) DO UPDATE
            SET total_casos = EXCLUDED.total_casos,
                id          = EXCLUDED.id
        """
    )
    conn.execute(sql, CASOS_DENGUE)
    return len(CASOS_DENGUE)


def run() -> None:
    engine = create_engine(DB_URL)
    with engine.begin() as conn:
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
