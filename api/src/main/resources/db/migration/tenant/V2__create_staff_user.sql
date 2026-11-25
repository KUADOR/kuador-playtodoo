-- Identity and access: staff of one sports complex (tenant schema). Each complex has its own isolated staff accounts:
-- the same e-mail in two complexes is two unrelated accounts, and a login on another complex never finds this one.
CREATE TABLE staff_user (
    id             uuid         PRIMARY KEY,
    email          varchar(254) NOT NULL UNIQUE,
    password_hash  varchar(100) NOT NULL,
    role           varchar(16)  NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'STAFF')),
    status         varchar(16)  NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at     timestamptz  NOT NULL,
    -- Logins look accounts up by the normalized (lower-case, trimmed) e-mail.
    CONSTRAINT staff_user_email_normalized CHECK (email = lower(btrim(email)))
);
