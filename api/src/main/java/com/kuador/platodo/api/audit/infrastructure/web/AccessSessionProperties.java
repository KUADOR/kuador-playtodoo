package com.kuador.platodo.api.audit.infrastructure.web;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the Kuador support sessions ({@value #PREFIX}.*; the prefix keeps the name of ADR-011).
 *
 * @param timeToLive lifetime of a support session; a security setting, not a business rule (ADR-011, ADR-020)
 */
@ConfigurationProperties(AccessSessionProperties.PREFIX)
record AccessSessionProperties(Duration timeToLive) {

    static final String PREFIX = "playtodoo.audit.access-session";

    AccessSessionProperties {
        Objects.requireNonNull(timeToLive, PREFIX + ".time-to-live");
    }
}
