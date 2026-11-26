package com.kuador.platodo.api.identity.infrastructure.persistence.global;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.CapabilityToggle;

import jakarta.persistence.EntityManager;

/** Capability change log through the global persistence unit; only capabilities this code knows are listed. */
public final class JpaCapabilityChangeLog implements CapabilityChangeLog {

    private static final List<String> KNOWN_KEYS = Arrays.stream(Capability.values()).map(Capability::name).toList();

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaCapabilityChangeLog(EntityManager global) {
        this.global = global;
    }

    @Override
    public void append(List<CapabilityToggle> changes, UUID changedBy, Instant changedAt) {
        changes.forEach(change -> global.persist(new CapabilityChangeEntity(
                UUID.randomUUID(), change.capability().name(), change.enabled(), changedBy, changedAt)));
        global.flush();
    }

    @Override
    public ChangePage pageSince(Instant since, PageRequest page) {
        long total = global.createQuery("""
                        SELECT count(c) FROM CapabilityChangeEntity c
                        WHERE c.changedAt >= :since AND c.capabilityKey IN :known
                        """, Long.class)
                .setParameter("since", since)
                .setParameter("known", KNOWN_KEYS)
                .getSingleResult();
        List<RecordedChange> items = global.createQuery("""
                        SELECT c, u.displayName FROM CapabilityChangeEntity c
                        JOIN PlatformUserEntity u ON u.id = c.changedBy
                        WHERE c.changedAt >= :since AND c.capabilityKey IN :known
                        ORDER BY c.changedAt DESC, c.id
                        """, Object[].class)
                .setParameter("since", since)
                .setParameter("known", KNOWN_KEYS)
                .setFirstResult(Math.toIntExact(page.offset()))
                .setMaxResults(page.size())
                .getResultList()
                .stream()
                .map(row -> recordedChangeOf((CapabilityChangeEntity) row[0], (String) row[1]))
                .toList();
        return new ChangePage(items, total);
    }

    private static RecordedChange recordedChangeOf(CapabilityChangeEntity change, String changedByName) {
        return new RecordedChange(change.id(), Capability.valueOf(change.capabilityKey()), change.enabled(),
                change.changedBy(), changedByName, change.changedAt());
    }
}
