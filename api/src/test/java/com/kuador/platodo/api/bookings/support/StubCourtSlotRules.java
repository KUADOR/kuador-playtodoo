package com.kuador.platodo.api.bookings.support;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.kuador.platodo.api.bookings.application.port.out.CourtSlotRules;

/** Courts with a configured slot duration; in this stub a valid slot starts on the hour or half hour. */
public final class StubCourtSlotRules implements CourtSlotRules {

    private static final long SLOT_START_MINUTES = 30;

    private final Map<UUID, Duration> durations = new ConcurrentHashMap<>();

    public UUID addCourt(Duration slotDuration) {
        UUID courtId = UUID.randomUUID();
        durations.put(courtId, slotDuration);
        return courtId;
    }

    @Override
    public Optional<Duration> slotDurationOf(UUID courtId) {
        return Optional.ofNullable(durations.get(courtId));
    }

    @Override
    public boolean isValidSlotStart(UUID courtId, Instant start) {
        return durations.containsKey(courtId)
                && start.truncatedTo(ChronoUnit.MINUTES).equals(start)
                && start.atZone(ZoneOffset.UTC).getMinute() % SLOT_START_MINUTES == 0;
    }
}
