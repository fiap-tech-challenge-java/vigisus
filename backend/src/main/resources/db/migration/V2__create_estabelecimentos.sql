CREATE TABLE estabelecimentos (
    id           BIGINT PRIMARY KEY,
    co_cnes      VARCHAR(7)  NOT NULL UNIQUE,
    no_fantasia  VARCHAR,
    co_municipio VARCHAR(7),
    nu_latitude  DECIMAL(9, 6),
    nu_longitude DECIMAL(9, 6),
    nu_telefone  VARCHAR,
    tp_gestao    VARCHAR,
    competencia  VARCHAR(6),
    updated_at   TIMESTAMP
);
