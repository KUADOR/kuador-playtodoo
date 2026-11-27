-- Plans and subscriptions: plan and status changes of every subscription, with who made them and when (PLATODO-85;
-- PLATODO-27 CA5, CA6). before is null for the first assignment, written with the registration. Kept for 365 days: the
-- list never returns older entries and a scheduled job deletes them (PLATODO-87). Complexes registered before this
-- migration have no registration entries; their history starts with their next change.
CREATE TABLE subscription_history (
    id          uuid         PRIMARY KEY,
    tenant_id   uuid         NOT NULL REFERENCES subscription (tenant_id),
    kind        varchar(8)   NOT NULL CHECK (kind IN ('PLAN', 'STATUS')),
    before      varchar(120),
    after       varchar(120) NOT NULL,
    changed_by  uuid         NOT NULL REFERENCES platform_user (id),
    changed_at  timestamptz  NOT NULL
);

-- The history of a complex is read newest first; the retention job deletes by date.
CREATE INDEX subscription_history_newest_idx ON subscription_history (tenant_id, changed_at DESC, id);
CREATE INDEX subscription_history_changed_at_idx ON subscription_history (changed_at);
