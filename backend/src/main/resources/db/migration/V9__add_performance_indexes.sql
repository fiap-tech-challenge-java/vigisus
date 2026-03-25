-- ============================================================
-- V9: Adiciona índices para performance com 50K+ registros
-- ============================================================

-- ==========================
-- MUNICIPIOS - Filtros por UF
-- ==========================
CREATE INDEX idx_municipios_sg_uf 
    ON municipios(sg_uf);

CREATE INDEX idx_municipios_co_ibge 
    ON municipios(co_ibge);

CREATE INDEX idx_municipios_no_municipio 
    ON municipios(no_municipio);

-- ==========================
-- CASOS_DENGUE - Queries mais comuns
-- ==========================
-- Buscar casos por município e ano
CREATE INDEX idx_casos_dengue_co_municipio_ano 
    ON casos_dengue(co_municipio, ano);

-- Buscar casos por ano (para agregação Brasil)
CREATE INDEX idx_casos_dengue_ano 
    ON casos_dengue(ano);

-- Buscar casos recentes por município
CREATE INDEX idx_casos_dengue_municipio_semana 
    ON casos_dengue(co_municipio, semana_epi DESC);

-- ==========================
-- ESTABELECIMENTOS - Busca por municipio
-- ==========================
CREATE INDEX idx_estabelecimentos_co_municipio 
    ON estabelecimentos(co_municipio);

CREATE INDEX idx_estabelecimentos_co_cnes 
    ON estabelecimentos(co_cnes);

-- ==========================
-- LEITOS - Busca por CNES
-- ==========================
CREATE INDEX idx_leitos_co_cnes 
    ON leitos(co_cnes);

-- ==========================
-- SERVICOS - Busca por CNES
-- ==========================
CREATE INDEX idx_servicos_co_cnes 
    ON servicos_especializados(co_cnes);
