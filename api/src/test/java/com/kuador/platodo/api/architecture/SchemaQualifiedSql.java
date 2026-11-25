package com.kuador.platodo.api.architecture;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.StringEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.Source;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit condition over the string constants of a class (SQL literals and text blocks end up there): it reports
 * SQL that names a schema, either by qualifying a table ({@code FROM global.tenant}) or by changing the search_path.
 * In tenant modules the schema comes only from the tenant scope (ADR-010).
 */
final class SchemaQualifiedSql {

    /** A table reference with a schema prefix after the SQL keywords that introduce one. */
    private static final Pattern QUALIFIED_TABLE = Pattern.compile(
            "(?i)\\b(?:from|join|into|update|table|truncate)\\s+(?:only\\s+)?\"?[a-z_][a-z0-9_$]*\"?\\s*\\.\\s*\"?[a-z_]");
    private static final Pattern SCHEMA_SWITCH = Pattern.compile("(?i)\\bsearch_path\\b|\\bset\\s+schema\\b");

    private SchemaQualifiedSql() {
    }

    static ArchCondition<JavaClass> notUseSchemaQualifiedSql() {
        return new ArchCondition<>("not use schema-qualified native SQL nor change the search_path") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (String sql : offendingConstants(javaClass)) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            javaClass.getName() + " names a schema in SQL: " + sql.strip()));
                }
            }
        };
    }

    static List<String> offendingConstants(JavaClass javaClass) {
        List<String> offending = new ArrayList<>();
        for (PoolEntry entry : ClassFile.of().parse(bytesOf(javaClass)).constantPool()) {
            if (entry instanceof StringEntry string && namesASchema(string.stringValue())) {
                offending.add(string.stringValue());
            }
        }
        return offending;
    }

    static boolean namesASchema(String text) {
        return QUALIFIED_TABLE.matcher(text).find() || SCHEMA_SWITCH.matcher(text).find();
    }

    private static byte[] bytesOf(JavaClass javaClass) {
        Source source = javaClass.getSource()
                .orElseThrow(() -> new IllegalStateException("No class file for " + javaClass.getName()));
        try (InputStream in = source.getUri().toURL().openStream()) {
            return in.readAllBytes();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
}
