package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.ComplexDetail;
import com.kuador.platodo.api.subscriptions.application.ComplexSummary;
import com.kuador.platodo.api.subscriptions.domain.LastPayment;
import com.kuador.platodo.api.subscriptions.domain.PlanSnapshot;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionStatus;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * Schemas {@code ComplexDetail}, {@code ComplexSummary} and {@code ComplexPage}. Every property is always present,
 * null when it does not apply; amounts follow {@link MoneyText}.
 */
final class ComplexResponses {

    /** The retention fields ({@code retentionDays}, {@code purgeDueAt}) are only shown while they run. */
    private static final Set<TenantStatus> IN_RETENTION = EnumSet.of(TenantStatus.SUSPENDED, TenantStatus.ARCHIVED);

    private ComplexResponses() {
    }

    /** Schema {@code ComplexDetail}. */
    record Detail(UUID id, String displayName, String subdomain, String ownerEmail, Instant createdAt,
            TenantStatus tenantStatus, Instant suspendedAt, Instant archivedAt, Integer retentionDays,
            Instant purgeDueAt, Instant purgedAt, SubscriptionView subscription,
            List<SubscriptionStatus> allowedTransitions, long version) {

        static Detail of(ComplexDetail complex) {
            TenantProfile tenant = complex.tenant();
            boolean inRetention = IN_RETENTION.contains(tenant.status());
            return new Detail(complex.subscription().tenantId(), tenant.displayName(), tenant.subdomain(),
                    tenant.ownerEmail(), tenant.createdAt(), tenant.status(), tenant.suspendedAt(),
                    tenant.archivedAt(), inRetention ? tenant.retentionDays() : null,
                    inRetention ? tenant.purgeDueAt() : null, tenant.purgedAt(),
                    SubscriptionView.of(complex.subscription(), complex.statusChangedByName()),
                    complex.allowedTransitions().stream().sorted().toList(), complex.version());
        }
    }

    /** Schema {@code Subscription}. */
    record SubscriptionView(SubscriptionStatus status, Instant statusSince, UserRef statusChangedBy,
            PlanSnapshotView plan) {

        static SubscriptionView of(Subscription subscription, String changedByName) {
            return new SubscriptionView(subscription.status(), subscription.statusSince(),
                    new UserRef(subscription.statusChangedBy(), changedByName),
                    PlanSnapshotView.of(subscription.plan()));
        }
    }

    /** Schema {@code UserRef}. */
    record UserRef(UUID id, String displayName) {
    }

    /** Schema {@code PlanSnapshot}. */
    record PlanSnapshotView(UUID planId, String name, String price, String currency, String periodicity,
            int courtLimit, int userLimit, Map<String, Object> customFields, Instant assignedAt) {

        static PlanSnapshotView of(PlanSnapshot plan) {
            return new PlanSnapshotView(plan.planId(), plan.name(), MoneyText.of(plan.price()), plan.currency(),
                    plan.periodicity(), plan.courtLimit(), plan.userLimit(), plan.customFields(), plan.assignedAt());
        }
    }

    /** Schema {@code ComplexSummary}. */
    record Summary(UUID id, String displayName, String subdomain, PlanRef plan, SubscriptionStatus subscriptionStatus,
            TenantStatus tenantStatus, Instant purgeDueAt, LastPaymentView lastPayment) {

        static Summary of(ComplexSummary complex) {
            TenantProfile tenant = complex.tenant();
            Subscription subscription = complex.subscription();
            PlanSnapshot plan = subscription.plan();
            return new Summary(subscription.tenantId(), tenant.displayName(), tenant.subdomain(),
                    new PlanRef(plan.planId(), plan.name()), subscription.status(), tenant.status(),
                    IN_RETENTION.contains(tenant.status()) ? tenant.purgeDueAt() : null,
                    complex.lastPayment().map(LastPaymentView::of).orElse(null));
        }
    }

    /** Schema {@code PlanRef}. */
    record PlanRef(UUID id, String name) {
    }

    /** Schema {@code LastPayment}. */
    record LastPaymentView(String amount, String currency, LocalDate paidOn) {

        static LastPaymentView of(LastPayment payment) {
            return new LastPaymentView(MoneyText.of(payment.amount()), payment.currency(), payment.paidOn());
        }
    }

    /** Schema {@code ComplexPage}. */
    record Page(List<Summary> items, int page, int size, long totalItems) {

        static Page of(ComplexSummary.Page complexes, int page, int size) {
            return new Page(complexes.items().stream().map(Summary::of).toList(), page, size,
                    complexes.totalItems());
        }
    }
}
