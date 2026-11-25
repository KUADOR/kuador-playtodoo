package com.kuador.platodo.api.architecture.fixtures;

/** Rule fixture: tenant SQL that relies on the scoped search_path, with table aliases. */
public final class UnqualifiedSqlRepository {

    public static final String SQL = """
            SELECT b.id, b.status FROM booking b
            JOIN booking other ON other.court_id = b.court_id
            WHERE b.id = ?
            """;

    private UnqualifiedSqlRepository() {
    }
}
