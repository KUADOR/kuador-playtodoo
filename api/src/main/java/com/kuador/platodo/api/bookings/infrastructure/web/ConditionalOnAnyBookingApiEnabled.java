package com.kuador.platodo.api.bookings.infrastructure.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.context.annotation.Conditional;

/** Registers the annotated bean when the player API, the staff API or both are enabled. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ConditionalOnAnyBookingApiEnabled.AnyBookingApiEnabled.class)
public @interface ConditionalOnAnyBookingApiEnabled {

    final class AnyBookingApiEnabled extends AnyNestedCondition {

        AnyBookingApiEnabled() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnPlayerApiEnabled
        static final class PlayerApi {
        }

        @ConditionalOnStaffApiEnabled
        static final class StaffApi {
        }
    }
}
