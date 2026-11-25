package com.kuador.platodo.api.bookings.infrastructure.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;

/**
 * Registers the annotated bean only when {@value #PROPERTY} is {@code true} (default {@code false}).
 * The staff API stays off until Tenancy resolves the tenant per request and Identity authenticates staff.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnBooleanProperty(ConditionalOnStaffApiEnabled.PROPERTY)
public @interface ConditionalOnStaffApiEnabled {

    String PROPERTY = "playtodoo.bookings.staff-api.enabled";
}
