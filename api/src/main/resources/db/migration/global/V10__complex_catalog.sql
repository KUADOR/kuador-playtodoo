-- Complexes and courts: global catalog of complexes (ADR-008, ADR-014; PLATODO-86). Only public data, so the search
-- of nearby complexes never reads a tenant schema. Tenancy keeps it aligned with the lifecycle through the synchronous
-- TenantStatusChanged: only ACTIVE complexes are visible. The purge deletes the row (ADR-016, PLATODO-87).
-- Address, location and contact are added with the search of nearby complexes: [POR DEFINIR: public data and sports].
CREATE TABLE complex_catalog (
    tenant_id     uuid         PRIMARY KEY REFERENCES tenant (id),
    subdomain     varchar(63)  NOT NULL,
    display_name  varchar(120) NOT NULL,
    visible       boolean      NOT NULL,
    updated_at    timestamptz  NOT NULL
);

-- Complexes that already exist: one row each while they have a current subdomain, visible only when ACTIVE.
INSERT INTO complex_catalog (tenant_id, subdomain, display_name, visible, updated_at)
SELECT t.id, d.subdomain, t.display_name, t.status = 'ACTIVE', now()
FROM tenant t
JOIN tenant_domain d ON d.tenant_id = t.id AND d.is_current;
