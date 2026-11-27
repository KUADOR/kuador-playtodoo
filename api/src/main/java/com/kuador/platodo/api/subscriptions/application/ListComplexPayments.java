package com.kuador.platodo.api.subscriptions.application;

import java.util.UUID;
import java.util.function.Function;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformUserNames;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionPayments;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionPayments.PaymentPage;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionPayment;

/**
 * {@code GET /complexes/{complexId}/payments}: every platform user, purged complexes included. A page costs the
 * payments, their count and one query for the names of who recorded them.
 */
public final class ListComplexPayments {

    private final CapabilityGuard guard;
    private final ComplexDetails details;
    private final SubscriptionPayments payments;
    private final PlatformUserNames userNames;

    public ListComplexPayments(CapabilityGuard guard, ComplexDetails details, SubscriptionPayments payments,
            PlatformUserNames userNames) {
        this.guard = guard;
        this.details = details;
        this.payments = payments;
        this.userNames = userNames;
    }

    /** @throws ComplexNotFoundException if no complex has this id */
    public RecordedPayment.Page list(UUID complexId, PageRequest page) {
        guard.requirePlatformUser();
        details.requireExists(complexId);
        PaymentPage found = payments.page(complexId, page);
        Function<UUID, String> names = PlatformUserNamesInBatch.of(userNames,
                found.items().stream().map(SubscriptionPayment::recordedBy).toList());
        return new RecordedPayment.Page(found.items().stream()
                .map(payment -> new RecordedPayment(payment, names.apply(payment.recordedBy())))
                .toList(), found.totalItems());
    }
}
