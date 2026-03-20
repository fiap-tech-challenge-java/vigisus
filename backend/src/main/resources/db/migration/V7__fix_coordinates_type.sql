-- Fix nu_latitude and nu_longitude columns type from DECIMAL to DOUBLE PRECISION
ALTER TABLE estabelecimentos
ALTER COLUMN nu_latitude TYPE DOUBLE PRECISION,
ALTER COLUMN nu_longitude TYPE DOUBLE PRECISION;

ALTER TABLE municipios
ALTER COLUMN nu_latitude TYPE DOUBLE PRECISION,
ALTER COLUMN nu_longitude TYPE DOUBLE PRECISION;

