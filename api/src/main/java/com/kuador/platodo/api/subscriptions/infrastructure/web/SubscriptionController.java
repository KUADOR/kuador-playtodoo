package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.subscriptions.application.ChangeComplexPlan;
import com.kuador.platodo.api.subscriptions.application.ChangeComplexRetention;
import com.kuador.platodo.api.subscriptions.application.ChangeSubscriptionStatus;
import com.kuador.platodo.api.subscriptions.application.ListComplexHistory;
import com.kuador.platodo.api.subscriptions.application.ListComplexPayments;
import com.kuador.platodo.api.subscriptions.application.RecordComplexPayment;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;

/**
 * Tag «Subscriptions» of {@code contracts/platform-backoffice.openapi.yaml}; global host. Payments are immutable:
 * there is no operation to edit or delete one.
 */
@RestController
@RequestMapping(PlansApiPaths.PLATFORM)
class SubscriptionController {

    static final String PLAN = ComplexController.COMPLEX + "/plan";
    static final String STATUS_CHANGES = ComplexController.COMPLEX + "/status-changes";
    static final String RETENTION = ComplexController.COMPLEX + "/retention";
    static final String PAYMENTS = ComplexController.COMPLEX + "/payments";
    static final String HISTORY = ComplexController.COMPLEX + "/history";

    private final ChangeComplexPlan changePlan;
    private final ChangeSubscriptionStatus changeStatus;
    private final ChangeComplexRetention changeRetention;
    private final RecordComplexPayment recordPayment;
    private final ListComplexPayments listPayments;
    private final ListComplexHistory listHistory;

    SubscriptionController(ChangeComplexPlan changePlan, ChangeSubscriptionStatus changeStatus,
            ChangeComplexRetention changeRetention, RecordComplexPayment recordPayment,
            ListComplexPayments listPayments, ListComplexHistory listHistory) {
        this.changePlan = changePlan;
        this.changeStatus = changeStatus;
        this.changeRetention = changeRetention;
        this.recordPayment = recordPayment;
        this.listPayments = listPayments;
        this.listHistory = listHistory;
    }

    @PutMapping(PLAN)
    ComplexResponses.Detail changePlan(@PathVariable UUID complexId, @RequestBody ChangePlanRequest request) {
        ChangePlanRequest complete = request.requireComplete();
        return ComplexResponses.Detail.of(changePlan.change(complexId, complete.planId(), complete.version()));
    }

    @PostMapping(STATUS_CHANGES)
    ComplexResponses.Detail changeStatus(@PathVariable UUID complexId, @RequestBody ChangeStatusRequest request) {
        ChangeStatusRequest complete = request.requireComplete();
        return ComplexResponses.Detail.of(
                changeStatus.change(complexId, complete.targetStatus(), complete.version()));
    }

    @PutMapping(RETENTION)
    ComplexResponses.Detail changeRetention(@PathVariable UUID complexId,
            @RequestBody ChangeRetentionRequest request) {
        ChangeRetentionRequest valid = request.requireValid();
        return ComplexResponses.Detail.of(
                changeRetention.change(complexId, valid.retentionDays(), valid.version()));
    }

    @PostMapping(PAYMENTS)
    @ResponseStatus(HttpStatus.CREATED)
    SubscriptionResponses.Payment recordPayment(@PathVariable UUID complexId,
            @RequestBody RecordPaymentRequest request) {
        return SubscriptionResponses.Payment.of(recordPayment.record(complexId, request.toDraft()));
    }

    @GetMapping(PAYMENTS)
    SubscriptionResponses.PaymentPage listPayments(@PathVariable UUID complexId,
            @RequestParam(defaultValue = PageParameters.FIRST_PAGE) int page,
            @RequestParam(defaultValue = PageParameters.DEFAULT_SIZE) int size) {
        PageRequest request = PageParameters.of(page, size);
        return SubscriptionResponses.PaymentPage.of(listPayments.list(complexId, request), request);
    }

    @GetMapping(HISTORY)
    SubscriptionResponses.HistoryPage listHistory(@PathVariable UUID complexId,
            @RequestParam(defaultValue = PageParameters.FIRST_PAGE) int page,
            @RequestParam(defaultValue = PageParameters.DEFAULT_SIZE) int size) {
        PageRequest request = PageParameters.of(page, size);
        return SubscriptionResponses.HistoryPage.of(listHistory.list(complexId, request), request);
    }
}
