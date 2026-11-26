package com.kuador.platodo.api.identity.infrastructure.web;

/** A body or parameter of a platform operation that breaks the contract ({@code 400 INVALID_REQUEST}). */
final class InvalidPlatformRequestException extends RuntimeException {

    InvalidPlatformRequestException(String message) {
        super(message);
    }
}
