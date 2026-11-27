package com.kuador.platodo.api.subscriptions.infrastructure.web;

/** A body or parameter of a plan or complex operation that breaks the contract ({@code 400 INVALID_REQUEST}). */
final class InvalidSubscriptionsRequestException extends RuntimeException {

    InvalidSubscriptionsRequestException(String message) {
        super(message);
    }
}
