package com.kuador.platodo.api.tenancy.infrastructure.web;

/** A request field breaks the limits of the contract; answered with {@code 400 INVALID_REQUEST}. */
final class InvalidRequestException extends IllegalArgumentException {

    InvalidRequestException(String message) {
        super(message);
    }
}
