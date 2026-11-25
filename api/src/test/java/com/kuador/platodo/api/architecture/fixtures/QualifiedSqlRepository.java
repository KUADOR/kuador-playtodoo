package com.kuador.platodo.api.architecture.fixtures;

/** Rule fixture: native SQL that qualifies a table with a schema. */
public final class QualifiedSqlRepository {

    public static final String SQL = """
            SELECT id FROM global.tenant WHERE status = 'ACTIVE'
            """;

    private QualifiedSqlRepository() {
    }
}
