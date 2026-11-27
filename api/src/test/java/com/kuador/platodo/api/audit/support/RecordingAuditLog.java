package com.kuador.platodo.api.audit.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog;
import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Audit trail kept in memory; {@link #failNextAppends()} makes appends fail like an unavailable database. */
public final class RecordingAuditLog implements AccessAuditLog {

    private final List<AccessAuditEvent> events = new CopyOnWriteArrayList<>();
    private volatile boolean failing;

    public void failNextAppends() {
        failing = true;
    }

    public List<AccessAuditEvent> events() {
        return List.copyOf(events);
    }

    @Override
    public void append(AccessAuditEvent event) {
        if (failing) {
            throw new IllegalStateException("Audit store unavailable");
        }
        events.add(event);
    }

    @Override
    public AuditPage pageOf(TenantId tenant, Instant since, PageRequest page) {
        List<AccessAuditEvent> ofTenant = new ArrayList<>(events.stream()
                .filter(event -> event.tenant().equals(tenant) && !event.occurredAt().isBefore(since))
                .toList());
        ofTenant.sort(Comparator.comparing(AccessAuditEvent::occurredAt).reversed());
        return new AuditPage(ofTenant.stream().skip(page.offset()).limit(page.size()).toList(), ofTenant.size());
    }
}
