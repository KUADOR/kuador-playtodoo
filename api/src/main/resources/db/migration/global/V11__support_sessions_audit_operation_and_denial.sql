-- Support access (ADR-017, ADR-020): the access sessions of ADR-011 become support sessions, opened by the
-- SUPER_ADMIN or an OPERATOR with TENANT_DATA_ACCESS. Every audited request now also keeps the operationId of the
-- contract operation and, when denied, the problem code answered. Existing rows are kept: their operationId is
-- unknown (NULL) and their denials were all answered for lack of a valid session.

ALTER TABLE access_audit_event
    ADD COLUMN operation_id varchar(100),
    ADD COLUMN denial_code  varchar(32);

UPDATE access_audit_event SET denial_code = 'SUPPORT_SESSION_INVALID' WHERE outcome = 'DENIED';

ALTER TABLE access_audit_event
    ADD CONSTRAINT access_audit_event_denial_code_known CHECK (denial_code IN
        ('CAPABILITY_REVOKED', 'TENANT_NOT_ACCESSIBLE', 'REASON_REQUIRED', 'SUPPORT_SESSION_INVALID')),
    ADD CONSTRAINT access_audit_event_denied_has_code CHECK ((outcome = 'DENIED') = (denial_code IS NOT NULL));
