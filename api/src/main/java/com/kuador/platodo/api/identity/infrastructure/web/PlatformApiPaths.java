package com.kuador.platodo.api.identity.infrastructure.web;

/** Paths of the Identity and access operations of {@code contracts/platform-backoffice.openapi.yaml}. */
final class PlatformApiPaths {

    static final String PLATFORM = "/api/v1/platform";
    static final String ME = "/me";
    static final String OPERATOR_CAPABILITIES = "/operator-capabilities";
    static final String OPERATOR_CAPABILITY_CHANGES = OPERATOR_CAPABILITIES + "/changes";

    private PlatformApiPaths() {
    }
}
