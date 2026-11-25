-- Audit (ADR-003, ADR-011): audited access of the Kuador owner to the data of one complex.
-- Both tables belong to the Audit module. The free-text reason may hold personal data despite the guidance of the
-- backoffice, so the purge of a complex replaces it and sets reason_redacted_at (ADR-013).

-- A time-boxed permission of one platform user on one complex, opened with a mandatory reason.
CREATE TABLE access_session (
    id                  uuid         PRIMARY KEY,
    platform_user_id    uuid         NOT NULL REFERENCES platform_user (id),
    tenant_id           uuid         NOT NULL REFERENCES tenant (id),
    reason              varchar(500) NOT NULL CHECK (btrim(reason) <> ''),
    started_at          timestamptz  NOT NULL,
    expires_at          timestamptz  NOT NULL,
    closed_at           timestamptz,
    reason_redacted_at  timestamptz,
    CONSTRAINT access_session_expires_after_start CHECK (expires_at > started_at)
);

CREATE INDEX access_session_tenant_idx ON access_session (tenant_id);

-- One row per request of a platform user on the host of a complex, written before the request runs. Insert only,
-- except for the anonymization of the reason when the complex is purged. Denied requests have no session nor reason.
-- route_template is the route pattern (for example /api/v1/staff/bookings/{bookingId}), never the concrete path
-- nor the query string.
CREATE TABLE access_audit_event (
    id                  uuid         PRIMARY KEY,
    session_id          uuid         REFERENCES access_session (id),
    platform_user_id    uuid         NOT NULL REFERENCES platform_user (id),
    tenant_id           uuid         NOT NULL REFERENCES tenant (id),
    reason              varchar(500),
    http_method         varchar(16)  NOT NULL,
    route_template      varchar(255) NOT NULL,
    outcome             varchar(8)   NOT NULL CHECK (outcome IN ('ALLOWED', 'DENIED')),
    occurred_at         timestamptz  NOT NULL,
    reason_redacted_at  timestamptz,
    CONSTRAINT access_audit_event_allowed_has_session CHECK (outcome = 'DENIED' OR session_id IS NOT NULL)
);

-- The audit of one complex is listed newest first.
CREATE INDEX access_audit_event_tenant_occurred_idx ON access_audit_event (tenant_id, occurred_at DESC, id);
