package com.kuador.platodo.api.identity.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog.ChangePage;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog.PageRequest;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog.RecordedChange;

/** Schema {@code CapabilityChangePage}. */
record CapabilityChangePageResponse(List<Item> items, int page, int size, long totalItems) {

    record UserRef(UUID id, String displayName) {
    }

    record Item(UUID id, String key, boolean enabled, UserRef changedBy, Instant changedAt) {

        static Item of(RecordedChange change) {
            return new Item(change.id(), change.capability().name(), change.enabled(),
                    new UserRef(change.changedBy(), change.changedByName()), change.changedAt());
        }
    }

    static CapabilityChangePageResponse of(ChangePage changes, PageRequest page) {
        return new CapabilityChangePageResponse(
                changes.items().stream().map(Item::of).toList(), page.page(), page.size(), changes.totalItems());
    }
}
