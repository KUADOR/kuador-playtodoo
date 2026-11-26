package com.kuador.platodo.api.subscriptions.infrastructure.web;

/** A body or parameter of a plan operation that breaks the contract ({@code 400 INVALID_REQUEST}). */
final class InvalidPlansRequestException extends RuntimeException {

    InvalidPlansRequestException(String message) {
        super(message);
    }
}
