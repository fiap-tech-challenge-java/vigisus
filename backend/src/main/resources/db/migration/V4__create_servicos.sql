CREATE TABLE servicos_especializados (
    id          BIGINT PRIMARY KEY,
    co_cnes     VARCHAR(7),
    serv_esp    VARCHAR(4),
    class_sr    VARCHAR(4),
    competencia VARCHAR(6)
);

CREATE INDEX idx_servicos_co_cnes_serv_esp ON servicos_especializados (co_cnes, serv_esp);
