package com.kuador.platodo.api.subscriptions.application;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformUserNames;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionHistory;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionHistory.HistoryPage;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionHistoryEntry;

/**
 * {@code GET /complexes/{complexId}/history}: every platform user; plan and status changes of the retention period,
 * newest first. Older entries are never returned, even before the scheduled job deletes them. A page costs the
 * entries, their count and one query for the names of who made them.
 */
public final class ListComplexHistory {

    /** History is kept for one year (PLATODO-27, decision of the Kuador team). */
    static final Duration RETENTION = Duration.ofDays(365);

    private final CapabilityGuard guard;
    private final ComplexDetails details;
    private final SubscriptionHistory history;
    private final PlatformUserNames userNames;
    private final Clock clock;

    public ListComplexHistory(CapabilityGuard guard, ComplexDetails details, SubscriptionHistory history,
            PlatformUserNames userNames, Clock clock) {
        this.guard = guard;
        this.details = details;
        this.history = history;
        this.userNames = userNames;
        this.clock = clock;
    }

    /** @throws ComplexNotFoundException if no complex has this id */
    public HistoryItem.Page list(UUID complexId, PageRequest page) {
        guard.requirePlatformUser();
        details.requireExists(complexId);
        HistoryPage found = history.page(complexId, clock.instant().minus(RETENTION), page);
        Function<UUID, String> names = PlatformUserNamesInBatch.of(userNames,
                found.items().stream().map(SubscriptionHistoryEntry::changedBy).toList());
        return new HistoryItem.Page(found.items().stream()
                .map(entry -> new HistoryItem(entry, names.apply(entry.changedBy())))
                .toList(), found.totalItems());
    }
}
