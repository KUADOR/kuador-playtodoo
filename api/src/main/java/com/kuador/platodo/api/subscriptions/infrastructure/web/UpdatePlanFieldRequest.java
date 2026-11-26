package com.kuador.platodo.api.subscriptions.infrastructure.web;

/** Schema {@code UpdatePlanFieldRequest}. Missing {@code required} or {@code version} fail as invalid requests. */
record UpdatePlanFieldRequest(String label, Boolean required, Long version) {

    boolean requiredFlag() {
        if (required == null) {
            throw new InvalidPlansRequestException("`required` is required.");
        }
        return required;
    }

    long requiredVersion() {
        if (version == null || version < 0) {
            throw new InvalidPlansRequestException("`version` is required and must not be negative.");
        }
        return version;
    }
}
