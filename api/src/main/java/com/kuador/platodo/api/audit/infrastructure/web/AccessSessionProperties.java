package com.kuador.platodo.api.audit.infrastructure.web;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the Kuador access sessions ({@value #PREFIX}.*).
 *
 * @param timeToLive lifetime of an access session; a security setting, not a business rule (ADR-011)
 */
@ConfigurationProperties(AccessSessionProperties.PREFIX)
record AccessSessionProperties(Duration timeToLive) {

    static final String PREFIX = "playtodoo.audit.access-session";

    AccessSessionProperties {
        Objects.requireNonNull(timeToLive, PREFIX + ".time-to-live");
    }
}
