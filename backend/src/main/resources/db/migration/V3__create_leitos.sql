CREATE TABLE leitos (
    id          BIGINT PRIMARY KEY,
    co_cnes     VARCHAR(7)  NOT NULL,
    tp_leito    VARCHAR(4),
    ds_leito    VARCHAR,
    qt_exist    INT,
    qt_sus      INT,
    competencia VARCHAR(6),
    updated_at  TIMESTAMP
);

CREATE INDEX idx_leitos_co_cnes_tp_leito ON leitos (co_cnes, tp_leito);
