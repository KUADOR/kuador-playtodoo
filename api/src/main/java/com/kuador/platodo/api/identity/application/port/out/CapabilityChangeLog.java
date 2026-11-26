package com.kuador.platodo.api.identity.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.CapabilityToggle;

/** Record of every effective change to the capabilities of operators (table {@code capability_change}). */
public interface CapabilityChangeLog {

    /**
     * Records {@code changes}, made by {@code changedBy} at {@code changedAt}. Needs a transaction of the global
     * schema; joins the one that applies them.
     */
    void append(List<CapabilityToggle> changes, UUID changedBy, Instant changedAt);

    /** Changes made at or after {@code since}, newest first. */
    ChangePage pageSince(Instant since, PageRequest page);

    /** One change, with the visible name of who made it. */
    record RecordedChange(UUID id, Capability capability, boolean enabled, UUID changedBy, String changedByName,
            Instant changedAt) {

        public RecordedChange {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(capability, "capability");
            Objects.requireNonNull(changedBy, "changedBy");
            Objects.requireNonNull(changedByName, "changedByName");
            Objects.requireNonNull(changedAt, "changedAt");
        }
    }

    /** Zero-based page and its size. */
    record PageRequest(int page, int size) {

        public PageRequest {
            if (page < 0 || size < 1) {
                throw new IllegalArgumentException("page must be >= 0 and size >= 1");
            }
        }

        public long offset() {
            return (long) page * size;
        }
    }

    record ChangePage(List<RecordedChange> items, long totalItems) {

        public ChangePage {
            items = List.copyOf(items);
        }
    }
}
