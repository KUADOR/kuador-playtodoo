package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.HistoryItem;
import com.kuador.platodo.api.subscriptions.application.RecordedPayment;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.domain.PaymentMethod;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionHistoryEntry;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionPayment;
import com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexResponses.UserRef;

/** Schemas {@code Payment}, {@code PaymentPage}, {@code HistoryEntry} and {@code HistoryPage}. */
final class SubscriptionResponses {

    private SubscriptionResponses() {
    }

    /** Schema {@code Payment}. */
    record Payment(UUID id, String amount, String currency, LocalDate paidOn, PaymentMethod method,
            UserRef recordedBy, Instant recordedAt) {

        static Payment of(RecordedPayment recorded) {
            SubscriptionPayment payment = recorded.payment();
            return new Payment(payment.id(), MoneyText.of(payment.amount()), payment.currency(), payment.paidOn(),
                    payment.method(), new UserRef(payment.recordedBy(), recorded.recordedByName()),
                    payment.recordedAt());
        }
    }

    /** Schema {@code PaymentPage}. */
    record PaymentPage(List<Payment> items, int page, int size, long totalItems) {

        static PaymentPage of(RecordedPayment.Page payments, PageRequest request) {
            return new PaymentPage(payments.items().stream().map(Payment::of).toList(), request.page(),
                    request.size(), payments.totalItems());
        }
    }

    /** Schema {@code HistoryEntry}. */
    record HistoryEntry(UUID id, SubscriptionHistoryEntry.Kind kind, String before, String after, UserRef changedBy,
            Instant changedAt) {

        static HistoryEntry of(HistoryItem item) {
            SubscriptionHistoryEntry entry = item.entry();
            return new HistoryEntry(entry.id(), entry.kind(), entry.before(), entry.after(),
                    new UserRef(entry.changedBy(), item.changedByName()), entry.changedAt());
        }
    }

    /** Schema {@code HistoryPage}. */
    record HistoryPage(List<HistoryEntry> items, int page, int size, long totalItems) {

        static HistoryPage of(HistoryItem.Page history, PageRequest request) {
            return new HistoryPage(history.items().stream().map(HistoryEntry::of).toList(), request.page(),
                    request.size(), history.totalItems());
        }
    }
}
