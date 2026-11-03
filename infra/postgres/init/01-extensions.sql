-- Runs once, on first initialisation of the data volume.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- column-level encryption if needed
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- fuzzy matching for regulatory codes
