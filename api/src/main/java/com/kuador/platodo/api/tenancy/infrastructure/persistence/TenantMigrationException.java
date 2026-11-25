package com.kuador.platodo.api.tenancy.infrastructure.persistence;

/** The tenant migrations of one schema failed; names the schema so operators know where to look. */
public class TenantMigrationException extends IllegalStateException {

    public TenantMigrationException(TenantSchemaName schema, Throwable cause) {
        super("Tenant migrations failed for schema " + schema.value(), cause);
    }
}
