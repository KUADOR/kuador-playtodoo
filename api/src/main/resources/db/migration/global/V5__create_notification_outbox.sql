-- Notifications (PLATODO-26 design, ADR-013): messages waiting for the e-mail provider of the notifications epic.
-- recipient is personal data, so a row is deleted once its message is sent. payload carries what the template needs
-- and never the recipient.
CREATE TABLE notification_outbox (
    id          uuid         PRIMARY KEY,
    type        varchar(64)  NOT NULL,
    recipient   varchar(254) NOT NULL,
    payload     jsonb        NOT NULL,
    created_at  timestamptz  NOT NULL,
    sent_at     timestamptz
);

-- The sender reads the pending messages oldest first.
CREATE INDEX notification_outbox_pending_idx ON notification_outbox (created_at) WHERE sent_at IS NULL;
