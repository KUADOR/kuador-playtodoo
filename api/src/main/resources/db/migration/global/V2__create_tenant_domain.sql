-- Tenancy: subdomains of each sports complex (ADR-012). The subdomain is the primary key, so a subdomain is never
-- reused, not even after it is retired (changed or purged complex). A complex has at most one current subdomain.
CREATE TABLE tenant_domain (
    subdomain   varchar(63) PRIMARY KEY,
    tenant_id   uuid        NOT NULL REFERENCES tenant (id),
    is_current  boolean     NOT NULL,
    created_at  timestamptz NOT NULL,
    retired_at  timestamptz,
    -- Same format as the api validates (ADR-012): lower case a-z, 0-9 and single inner hyphens, 3 to 63 characters.
    CONSTRAINT tenant_domain_subdomain_format CHECK (
        subdomain ~ '^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$' AND position('--' IN subdomain) = 0),
    CONSTRAINT tenant_domain_retired_when_not_current CHECK (is_current = (retired_at IS NULL))
);

CREATE UNIQUE INDEX tenant_domain_one_current_per_tenant ON tenant_domain (tenant_id) WHERE is_current;
