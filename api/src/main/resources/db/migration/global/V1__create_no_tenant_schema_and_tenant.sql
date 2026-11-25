-- Global schema (ADR-009, ADR-010). Flyway runs this set with search_path = global.

-- Empty schema: the search_path of every pooled connection outside a tenant scope. It must never hold tables,
-- so a connection reused without a tenant cannot read any tenant data.
CREATE SCHEMA IF NOT EXISTS no_tenant;

-- Tenancy: one row per sports complex. No personal data: the owner's e-mail lives in the tenant schema.
-- schema_name is immutable and derived from the id (t_ + UUID without hyphens), never from the subdomain.
CREATE TABLE tenant (
    id              uuid         PRIMARY KEY,
    schema_name     varchar(34)  NOT NULL UNIQUE,
    display_name    varchar(120) NOT NULL,
    status          varchar(16)  NOT NULL
        CHECK (status IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'ARCHIVED', 'PURGING', 'PURGED')),
    created_at      timestamptz  NOT NULL,
    activated_at    timestamptz,
    suspended_at    timestamptz,
    archived_at     timestamptz,
    retention_days  integer      CHECK (retention_days >= 1),
    purge_due_at    timestamptz,
    purged_at       timestamptz,
    version         bigint       NOT NULL DEFAULT 0,
    CONSTRAINT tenant_schema_name_from_id CHECK (schema_name = 't_' || replace(id::text, '-', ''))
);

-- Startup migrations list the tenants by status in creation order.
CREATE INDEX tenant_status_created_idx ON tenant (status, created_at);
