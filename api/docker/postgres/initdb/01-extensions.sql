-- Database-wide extensions shared by every tenant schema.
-- Idempotent: safe to run on an existing database (local, CI and non-production environments).
-- Holds no tenant data. Tenant connections must include "extensions" in their search_path
-- so that operator classes such as gist(uuid) resolve in EXCLUDE constraints.
CREATE SCHEMA IF NOT EXISTS extensions;
GRANT USAGE ON SCHEMA extensions TO PUBLIC;

-- btree_gist backs the booking no-overlap exclusion constraint (ADR-006).
-- Trusted extension since PostgreSQL 13: the database owner can create it without superuser.
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA extensions;
