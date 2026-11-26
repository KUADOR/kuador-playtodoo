package com.kuador.platodo.api.subscriptions.application;

import java.util.List;
import java.util.Objects;

import com.kuador.platodo.api.subscriptions.domain.SubscriptionHistoryEntry;

/** A plan or status change with the visible name of who made it. */
public record HistoryItem(SubscriptionHistoryEntry entry, String changedByName) {

    public HistoryItem {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(changedByName, "changedByName");
    }

    /** One page of the history of a complex, newest first. */
    public record Page(List<HistoryItem> items, long totalItems) {

        public Page {
            items = List.copyOf(items);
        }
    }
}
