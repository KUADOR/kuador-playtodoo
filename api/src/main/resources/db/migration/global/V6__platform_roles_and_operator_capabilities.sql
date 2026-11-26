-- Identity and access (ADR-017, PLATODO-82): Kuador roles SUPER_ADMIN and OPERATOR, and the capabilities that the
-- SUPER_ADMIN enables for operators. All tables belong to Identity and access.

-- Roles. The PLATFORM_OWNER of PLATODO-26 keeps every permission it had as SUPER_ADMIN.
ALTER TABLE platform_user DROP CONSTRAINT platform_user_role_check;
UPDATE platform_user SET role = 'SUPER_ADMIN' WHERE role = 'PLATFORM_OWNER';
ALTER TABLE platform_user
    ADD CONSTRAINT platform_user_role_check CHECK (role IN ('SUPER_ADMIN', 'OPERATOR'));

-- Visible name of a Kuador team member (GET /me and "changed by" in the backoffice). Existing users start with the
-- local part of their e-mail until account management exists.
ALTER TABLE platform_user ADD COLUMN display_name varchar(120);
UPDATE platform_user SET display_name = split_part(email, '@', 1);
ALTER TABLE platform_user ALTER COLUMN display_name SET NOT NULL;
ALTER TABLE platform_user
    ADD CONSTRAINT platform_user_display_name_not_blank CHECK (btrim(display_name) <> '');

-- Catalog of the capabilities defined in code (enum Capability). A new capability arrives with a migration that seeds
-- it here and, when delegable, in operator_capability disabled.
CREATE TABLE platform_capability (
    key          varchar(64)  PRIMARY KEY,
    description  varchar(200) NOT NULL,
    delegable    boolean      NOT NULL
);

INSERT INTO platform_capability (key, description, delegable) VALUES
    ('COMPLEX_CREATE', 'Register complexes and retry their provisioning.', true),
    ('SUBSCRIPTION_PAYMENT_RECORD', 'Record subscription payments.', true),
    ('SUBSCRIPTION_STATUS_CHANGE', 'Mark expiring, suspend, reactivate and cancel subscriptions.', true),
    ('TENANT_DATA_ACCESS',
     'Open support sessions with a reason on active complexes to read and edit their data.', true);

-- Whether operators have each delegable capability. Every capability starts disabled. The version of the whole set,
-- answered by GET /operator-capabilities, is the highest row version (optimistic concurrency).
CREATE TABLE operator_capability (
    capability_key  varchar(64) PRIMARY KEY REFERENCES platform_capability (key),
    enabled         boolean     NOT NULL DEFAULT false,
    version         bigint      NOT NULL DEFAULT 0 CHECK (version >= 0)
);

INSERT INTO operator_capability (capability_key)
SELECT key FROM platform_capability WHERE delegable;

-- One row per effective change of operator_capability. Insert only; kept 365 days (ADR-017).
CREATE TABLE capability_change (
    id              uuid        PRIMARY KEY,
    capability_key  varchar(64) NOT NULL REFERENCES platform_capability (key),
    enabled         boolean     NOT NULL,
    changed_by      uuid        NOT NULL REFERENCES platform_user (id),
    changed_at      timestamptz NOT NULL
);

CREATE INDEX capability_change_changed_at_idx ON capability_change (changed_at DESC);
