package com.kuador.platodo.api.subscriptions.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.PlanNameTakenException;
import com.kuador.platodo.api.subscriptions.domain.Plan;
import com.kuador.platodo.api.subscriptions.domain.PlanStatus;

/** Stored plans. Plan names are unique without regard to case. */
public interface Plans {

    Optional<Plan> find(UUID id);

    /** The plan, locked until the current transaction ends. */
    Optional<Plan> lock(UUID id);

    /** One page of plans ordered by name, optionally only those with {@code status}. */
    PlanPage page(Optional<PlanStatus> status, PageRequest page);

    /** @throws PlanNameTakenException if another plan has the same name */
    void insert(Plan plan);

    /** @throws PlanNameTakenException if another plan has the same name */
    void update(Plan plan);

    record PageRequest(int page, int size) {

        public long offset() {
            return (long) page * size;
        }
    }

    record PlanPage(List<Plan> items, long totalItems) {

        public PlanPage {
            items = List.copyOf(items);
        }
    }
}
