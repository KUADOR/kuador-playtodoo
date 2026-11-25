package com.kuador.platodo.api.support.infrastructure.persistence.tenant;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Test-only entity of the tenant persistence unit (its package matches the tenant entity packages), used to exercise
 * Hibernate multi-tenancy before the first production tenant entity exists. Its table is created by the test.
 */
@Entity
@Table(name = ProbeNote.TABLE)
public class ProbeNote {

    public static final String TABLE = "probe_note";
    public static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (id uuid PRIMARY KEY, text varchar(80) NOT NULL)";

    @Id
    private UUID id;
    private String text;

    protected ProbeNote() {
    }

    public ProbeNote(UUID id, String text) {
        this.id = id;
        this.text = text;
    }

    public UUID id() {
        return id;
    }

    public String text() {
        return text;
    }
}
