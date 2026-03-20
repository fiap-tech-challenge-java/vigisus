-- ============================================================
-- V8: Corrige colunas id para BIGSERIAL e adiciona FKs
-- ============================================================

-- 1. Corrige casos_dengue.id para BIGSERIAL
ALTER TABLE casos_dengue ALTER COLUMN id DROP DEFAULT;
CREATE SEQUENCE IF NOT EXISTS casos_dengue_id_seq;
ALTER TABLE casos_dengue ALTER COLUMN id
    SET DEFAULT nextval('casos_dengue_id_seq');
SELECT setval('casos_dengue_id_seq', COALESCE((SELECT MAX(id) FROM casos_dengue), 0) + 1);

-- 2. Corrige estabelecimentos.id para BIGSERIAL
ALTER TABLE estabelecimentos ALTER COLUMN id DROP DEFAULT;
CREATE SEQUENCE IF NOT EXISTS estabelecimentos_id_seq;
ALTER TABLE estabelecimentos ALTER COLUMN id
    SET DEFAULT nextval('estabelecimentos_id_seq');
SELECT setval('estabelecimentos_id_seq', COALESCE((SELECT MAX(id) FROM estabelecimentos), 0) + 1);

-- 3. Corrige leitos.id para BIGSERIAL
ALTER TABLE leitos ALTER COLUMN id DROP DEFAULT;
CREATE SEQUENCE IF NOT EXISTS leitos_id_seq;
ALTER TABLE leitos ALTER COLUMN id
    SET DEFAULT nextval('leitos_id_seq');
SELECT setval('leitos_id_seq', COALESCE((SELECT MAX(id) FROM leitos), 0) + 1);

-- 4. Corrige servicos_especializados.id para BIGSERIAL
ALTER TABLE servicos_especializados ALTER COLUMN id DROP DEFAULT;
CREATE SEQUENCE IF NOT EXISTS servicos_especializados_id_seq;
ALTER TABLE servicos_especializados ALTER COLUMN id
    SET DEFAULT nextval('servicos_especializados_id_seq');
SELECT setval('servicos_especializados_id_seq', COALESCE((SELECT MAX(id) FROM servicos_especializados), 0) + 1);

-- ============================================================
-- Foreign Keys
-- ============================================================

-- casos_dengue.co_municipio → municipios.co_ibge
ALTER TABLE casos_dengue
    ADD CONSTRAINT fk_casos_dengue_municipio
    FOREIGN KEY (co_municipio)
    REFERENCES municipios(co_ibge)
    ON DELETE CASCADE;

-- estabelecimentos.co_municipio → municipios.co_ibge
ALTER TABLE estabelecimentos
    ADD CONSTRAINT fk_estabelecimentos_municipio
    FOREIGN KEY (co_municipio)
    REFERENCES municipios(co_ibge)
    ON DELETE CASCADE;

-- leitos.co_cnes → estabelecimentos.co_cnes
ALTER TABLE leitos
    ADD CONSTRAINT fk_leitos_estabelecimento
    FOREIGN KEY (co_cnes)
    REFERENCES estabelecimentos(co_cnes)
    ON DELETE CASCADE;

-- servicos_especializados.co_cnes → estabelecimentos.co_cnes
ALTER TABLE servicos_especializados
    ADD CONSTRAINT fk_servicos_estabelecimento
    FOREIGN KEY (co_cnes)
    REFERENCES estabelecimentos(co_cnes)
    ON DELETE CASCADE;
