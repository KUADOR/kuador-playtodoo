-- Bookings module: reservations and slot blocks of one sports complex (tenant schema).
-- ADR-006: an active booking (PENDING or CONFIRMED) never overlaps another one on the same court.
-- The gist(uuid) operator class comes from btree_gist in the shared "extensions" schema,
-- so the connection search_path must include it (see api/README.md).
CREATE TABLE booking (
    id            uuid         PRIMARY KEY,
    court_id      uuid         NOT NULL,
    kind          varchar(16)  NOT NULL CHECK (kind IN ('RESERVATION', 'BLOCK')),
    status        varchar(16)  NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    period        tstzrange    NOT NULL CHECK (NOT isempty(period) AND lower_inc(period) AND NOT upper_inc(period)),
    player_id     uuid,
    holder_name   varchar(120),
    holder_phone  varchar(32),
    block_reason  varchar(200),
    created_by    uuid         NOT NULL,
    created_at    timestamptz  NOT NULL,
    expires_at    timestamptz,
    confirmed_at  timestamptz,
    cancelled_at  timestamptz,
    cancelled_by  uuid,
    version       bigint       NOT NULL DEFAULT 0,
    CONSTRAINT booking_pending_has_expiry CHECK (status <> 'PENDING' OR expires_at IS NOT NULL),
    CONSTRAINT booking_block_never_pending CHECK (kind <> 'BLOCK' OR status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT booking_no_overlap EXCLUDE USING gist (court_id WITH =, period WITH &&)
        WHERE (status IN ('PENDING', 'CONFIRMED'))
);

-- Expiry job and lazy expiry look up pending bookings by deadline.
CREATE INDEX booking_pending_expiry_idx ON booking (expires_at) WHERE status = 'PENDING';
-- Own-overlap warning looks up a player's active bookings.
CREATE INDEX booking_active_player_idx ON booking (player_id) WHERE status IN ('PENDING', 'CONFIRMED');
