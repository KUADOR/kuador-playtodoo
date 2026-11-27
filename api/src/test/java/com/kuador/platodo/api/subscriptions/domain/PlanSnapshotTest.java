package com.kuador.platodo.api.subscriptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** CA3 and ADR-018: only a published plan is assigned, and its conditions are copied when it is. */
class PlanSnapshotTest {

    private static final Instant AT = Instant.parse("2026-12-01T14:00:00Z");
    private static final PlanContent COMPLETE = new PlanContent("Premium", new BigDecimal("120.00"), "USD", "MONTHLY",
            10, 25, Map.of("support-hours", "Weekdays 9 to 18"));

    @Test
    void aPublishedPlanIsCopiedWithItsConditions() {
        Plan published = new Plan(UUID.randomUUID(), COMPLETE, PlanStatus.PUBLISHED, 2);

        PlanSnapshot snapshot = PlanSnapshot.of(published, AT);

        assertThat(snapshot.planId()).isEqualTo(published.id());
        assertThat(snapshot.name()).isEqualTo("Premium");
        assertThat(snapshot.price()).isEqualByComparingTo("120");
        assertThat(snapshot.courtLimit()).isEqualTo(10);
        assertThat(snapshot.userLimit()).isEqualTo(25);
        assertThat(snapshot.customFields()).containsEntry("support-hours", "Weekdays 9 to 18");
        assertThat(snapshot.assignedAt()).isEqualTo(AT);
    }

    @Test
    void aDraftIsNotAssigned() {
        Plan draft = new Plan(UUID.randomUUID(), COMPLETE, PlanStatus.DRAFT, 1);

        assertThatThrownBy(() -> PlanSnapshot.of(draft, AT)).isInstanceOf(PlanNotPublishedException.class);
    }
}
