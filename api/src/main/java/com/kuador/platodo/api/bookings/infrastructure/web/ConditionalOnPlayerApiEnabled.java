package com.kuador.platodo.api.bookings.infrastructure.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;

/**
 * Registers the annotated bean only when {@value #PROPERTY} is {@code true} (default {@code false}).
 * The player API stays off until Tenancy resolves the tenant per request and Identity authenticates players.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnBooleanProperty(ConditionalOnPlayerApiEnabled.PROPERTY)
public @interface ConditionalOnPlayerApiEnabled {

    String PROPERTY = "playtodoo.bookings.player-api.enabled";
}
