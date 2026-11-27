-- Registration of complexes with their subscription (ADR-014, ADR-016, ADR-018; PLATODO-84).

-- Tenancy: a registration whose schema could not be created waits in PROVISIONING_FAILED for a retry.
ALTER TABLE tenant DROP CONSTRAINT tenant_status_check;
ALTER TABLE tenant ALTER COLUMN status TYPE varchar(20);
ALTER TABLE tenant ADD CONSTRAINT tenant_status_check CHECK (status IN (
    'PROVISIONING', 'PROVISIONING_FAILED', 'ACTIVE', 'SUSPENDED', 'ARCHIVED', 'PURGING', 'PURGED'));

-- Tenancy: e-mail of the owner account given at registration, normalized (trimmed, lower case). Personal data: the
-- backoffice shows it in the complex detail, a provisioning retry creates the owner from it, and the purge redacts it
-- (ADR-016). Null for complexes registered before this migration and once purged.
ALTER TABLE tenant ADD COLUMN owner_email varchar(254);

-- Plans and subscriptions: one subscription per complex, created ACTIVE with the registration. plan_snapshot fixes the
-- conditions of the plan when it was assigned (ADR-018); plan_id is the plan it came from. Status changes are manual
-- (ADR-014). version supports optimistic concurrency.
CREATE TABLE subscription (
    id                 uuid         PRIMARY KEY,
    tenant_id          uuid         NOT NULL UNIQUE REFERENCES tenant (id),
    plan_id            uuid         NOT NULL REFERENCES plan (id),
    plan_snapshot      jsonb        NOT NULL CHECK (jsonb_typeof(plan_snapshot) = 'object'),
    status             varchar(16)  NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRING', 'SUSPENDED', 'CANCELLED')),
    status_since       timestamptz  NOT NULL,
    status_changed_by  uuid         NOT NULL REFERENCES platform_user (id),
    created_at         timestamptz  NOT NULL,
    version            bigint       NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- The complex list is newest first.
CREATE INDEX subscription_created_idx ON subscription (created_at DESC, tenant_id);

-- Plans and subscriptions: payments recorded by hand (no automatic charge, ADR-004). Immutable. The list of complexes
-- reads the latest one per complex; recording them is PLATODO-85. Legal retention: [POR DEFINIR: obligations of the
-- country].
CREATE TABLE subscription_payment (
    id           uuid           PRIMARY KEY,
    tenant_id    uuid           NOT NULL REFERENCES subscription (tenant_id),
    amount       numeric(19, 4) NOT NULL CHECK (amount > 0),
    currency     char(3)        NOT NULL,
    paid_on      date           NOT NULL,
    method       varchar(16)    NOT NULL CHECK (method IN ('TRANSFER', 'INVOICE')),
    recorded_by  uuid           NOT NULL REFERENCES platform_user (id),
    recorded_at  timestamptz    NOT NULL
);

CREATE INDEX subscription_payment_latest_idx ON subscription_payment (tenant_id, paid_on DESC, recorded_at DESC, id);
