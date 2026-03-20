-- Fix total_casos column type from INT to BIGINT
ALTER TABLE casos_dengue
ALTER COLUMN total_casos TYPE BIGINT;

