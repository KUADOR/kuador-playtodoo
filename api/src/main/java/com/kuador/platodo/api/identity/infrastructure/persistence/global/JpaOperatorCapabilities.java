package com.kuador.platodo.api.identity.infrastructure.persistence.global;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.kuador.platodo.api.identity.application.port.out.OperatorCapabilities;
import com.kuador.platodo.api.identity.application.port.out.OperatorCapabilityUpdates;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.CapabilityToggle;
import com.kuador.platodo.api.identity.domain.OperatorCapabilitySettings;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/**
 * Capabilities enabled for operators, through the global persistence unit. The version of the settings is the highest
 * row version. Rows of capabilities this code does not know are ignored.
 */
public final class JpaOperatorCapabilities implements OperatorCapabilities, OperatorCapabilityUpdates {

    private static final String ALL_ROWS = """
            SELECT c FROM OperatorCapabilityEntity c ORDER BY c.capabilityKey
            """;

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaOperatorCapabilities(EntityManager global) {
        this.global = global;
    }

    @Override
    public OperatorCapabilitySettings current() {
        return settingsOf(global.createQuery(ALL_ROWS, OperatorCapabilityEntity.class).getResultList());
    }

    /** Locks every row in key order, so two concurrent changes never deadlock. */
    @Override
    public OperatorCapabilitySettings lockCurrent() {
        return settingsOf(global.createQuery(ALL_ROWS, OperatorCapabilityEntity.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList());
    }

    @Override
    public void apply(List<CapabilityToggle> changes, long newVersion) {
        for (CapabilityToggle change : changes) {
            OperatorCapabilityEntity row = global.find(OperatorCapabilityEntity.class, change.capability().name());
            if (row == null) {
                throw new IllegalStateException("Capability " + change.capability() + " is not seeded for operators");
            }
            row.change(change.enabled(), newVersion);
        }
        global.flush();
    }

    private static OperatorCapabilitySettings settingsOf(List<OperatorCapabilityEntity> rows) {
        Map<Capability, Boolean> enabled = new EnumMap<>(Capability.class);
        long version = 0;
        for (OperatorCapabilityEntity row : rows) {
            Capability.ofKey(row.capabilityKey()).ifPresent(capability -> enabled.put(capability, row.enabled()));
            version = Math.max(version, row.version());
        }
        return new OperatorCapabilitySettings(version, enabled);
    }
}
