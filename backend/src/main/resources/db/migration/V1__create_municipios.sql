CREATE TABLE municipios (
    id          BIGINT PRIMARY KEY,
    co_ibge     VARCHAR(7)     NOT NULL UNIQUE,
    no_municipio VARCHAR        NOT NULL,
    sg_uf       VARCHAR(2)     NOT NULL,
    nu_latitude  DECIMAL(9, 6),
    nu_longitude DECIMAL(9, 6),
    populacao   BIGINT,
    updated_at  TIMESTAMP
);
