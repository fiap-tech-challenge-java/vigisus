CREATE TABLE casos_dengue (
    id           BIGINT PRIMARY KEY,
    co_municipio VARCHAR(7)  NOT NULL,
    ano          INT         NOT NULL,
    semana_epi   INT         NOT NULL,
    total_casos  INT,
    updated_at   TIMESTAMP,
    CONSTRAINT uq_casos_dengue UNIQUE (co_municipio, ano, semana_epi)
);
