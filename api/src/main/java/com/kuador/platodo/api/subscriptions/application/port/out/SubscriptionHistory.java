package com.kuador.platodo.api.subscriptions.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionHistoryEntry;

/** Plan and status changes of every subscription. Appended inside the global transaction of the change. */
public interface SubscriptionHistory {

    void append(List<SubscriptionHistoryEntry> entries);

    /** One page of the changes of the complex {@code tenantId} made at {@code since} or later, newest first. */
    HistoryPage page(UUID tenantId, Instant since, PageRequest page);

    record HistoryPage(List<SubscriptionHistoryEntry> items, long totalItems) {

        public HistoryPage {
            items = List.copyOf(items);
        }
    }
}
