-- Plans and subscriptions (ADR-018, PLATODO-83): commercial plans with typed basic fields and custom fields stored as
-- jsonb, validated against plan_field_definition. Both tables belong to Plans and subscriptions.

-- Platform parameters: values the Kuador team configures without a code change. Each later task seeds its own keys
-- (retention, audit, support session). The value is jsonb so a parameter can be a number, a text or a list.
CREATE TABLE platform_parameter (
    key          varchar(100) PRIMARY KEY,
    value        jsonb        NOT NULL,
    description  varchar(200) NOT NULL
);

-- Allowed currencies (ISO 4217) and periodicities of a plan, answered by GET /plan-options.
-- [POR DEFINIR: values]. They start empty, so no plan can set a currency or a periodicity (nor be published) until
-- the Kuador team sets them; see docs/deployment/changes/PLATODO-83.md.
INSERT INTO platform_parameter (key, value, description) VALUES
    ('plan.currencies', '[]', 'ISO 4217 codes a plan may use.'),
    ('plan.periodicities', '[]', 'Billing periodicities a plan may use.');

-- Field definitions. BASIC rows describe the typed columns of plan: seeded here, read-only and never retired.
-- CUSTOM rows are created by the SUPER_ADMIN; their key names the entry in plan.custom_fields and never changes, and
-- their type is immutable. A retired field is no longer offered nor required, but existing values are kept.
CREATE TABLE plan_field_definition (
    id           uuid         PRIMARY KEY,
    key          varchar(80)  NOT NULL,
    label        varchar(80)  NOT NULL CHECK (btrim(label) <> ''),
    type         varchar(16)  NOT NULL CHECK (type IN ('TEXT', 'NUMBER', 'BOOLEAN')),
    required     boolean      NOT NULL,
    origin       varchar(16)  NOT NULL CHECK (origin IN ('BASIC', 'CUSTOM')),
    basic_order  smallint,
    retired_at   timestamptz,
    version      bigint       NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT plan_field_definition_basic_order_check CHECK ((origin = 'BASIC') = (basic_order IS NOT NULL)),
    CONSTRAINT plan_field_definition_basic_not_retired_check CHECK (origin = 'CUSTOM' OR retired_at IS NULL)
);

CREATE UNIQUE INDEX plan_field_definition_key_unique ON plan_field_definition (key);
CREATE UNIQUE INDEX plan_field_definition_label_unique ON plan_field_definition (lower(label));

INSERT INTO plan_field_definition (id, key, label, type, required, origin, basic_order) VALUES
    (gen_random_uuid(), 'name', 'Name', 'TEXT', true, 'BASIC', 1),
    (gen_random_uuid(), 'price', 'Price', 'NUMBER', true, 'BASIC', 2),
    (gen_random_uuid(), 'currency', 'Currency', 'TEXT', true, 'BASIC', 3),
    (gen_random_uuid(), 'periodicity', 'Periodicity', 'TEXT', true, 'BASIC', 4),
    (gen_random_uuid(), 'courtLimit', 'Court limit', 'NUMBER', true, 'BASIC', 5),
    (gen_random_uuid(), 'userLimit', 'User limit', 'NUMBER', true, 'BASIC', 6);

-- Plans. A draft needs only its name; publishing requires every basic field and every required, non-retired custom
-- field. Unpublishing and deleting plans are out of scope. version supports optimistic concurrency.
CREATE TABLE plan (
    id             uuid           PRIMARY KEY,
    name           varchar(120)   NOT NULL CHECK (btrim(name) <> ''),
    price          numeric(19, 4) CHECK (price >= 0),
    currency       char(3),
    periodicity    varchar(32),
    court_limit    integer        CHECK (court_limit >= 0),
    user_limit     integer        CHECK (user_limit >= 0),
    custom_fields  jsonb          NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(custom_fields) = 'object'),
    status         varchar(16)    NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED')),
    version        bigint         NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT plan_published_complete_check CHECK (status = 'DRAFT' OR (price IS NOT NULL AND currency IS NOT NULL
        AND periodicity IS NOT NULL AND court_limit IS NOT NULL AND user_limit IS NOT NULL))
);

CREATE UNIQUE INDEX plan_name_unique ON plan (lower(name));
CREATE INDEX plan_status_idx ON plan (status);
