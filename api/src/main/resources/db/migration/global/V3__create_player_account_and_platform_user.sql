-- Identity and access (PLATODO-26 design): accounts that live in the global schema.
-- Staff accounts are not here: they live in the schema of their complex (tenant migration V2__create_staff_user).

-- Global account of a player: one account works on every complex. Operational tables of a complex keep only the
-- player_account id (no foreign key across schemas) and never copy the e-mail or the name.
CREATE TABLE player_account (
    id             uuid         PRIMARY KEY,
    email          varchar(254) NOT NULL UNIQUE,
    password_hash  varchar(100) NOT NULL,
    display_name   varchar(120) NOT NULL,
    status         varchar(16)  NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at     timestamptz  NOT NULL,
    -- Logins look accounts up by the normalized (lower-case, trimmed) e-mail.
    CONSTRAINT player_account_email_normalized CHECK (email = lower(btrim(email)))
);

-- Kuador team member. PLATFORM_OWNER is the only role with access to complex data (ADR-003); other platform roles
-- arrive with their own decision and migration.
CREATE TABLE platform_user (
    id             uuid         PRIMARY KEY,
    email          varchar(254) NOT NULL UNIQUE,
    password_hash  varchar(100) NOT NULL,
    role           varchar(32)  NOT NULL CHECK (role IN ('PLATFORM_OWNER')),
    status         varchar(16)  NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at     timestamptz  NOT NULL,
    CONSTRAINT platform_user_email_normalized CHECK (email = lower(btrim(email)))
);
