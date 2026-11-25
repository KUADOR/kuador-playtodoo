package com.kuador.platodo.api.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class that proves tenant isolation for the named tenant tables: data of tenant A is neither read nor
 * changed from tenant B. Every table created by the tenant migrations, whether mapped by JPA or written with JDBC,
 * needs one; {@code TenantEntityCoverageIT} fails {@code ./mvnw -B verify} otherwise (ADR-010).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CoversTenantEntity {

    /** Tenant table names, as created by {@code db/migration/tenant}. */
    String[] value();
}
