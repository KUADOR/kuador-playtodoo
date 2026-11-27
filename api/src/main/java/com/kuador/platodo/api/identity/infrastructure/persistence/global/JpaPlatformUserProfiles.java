package com.kuador.platodo.api.identity.infrastructure.persistence.global;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.kuador.platodo.api.identity.application.port.in.PlatformUserNames;
import com.kuador.platodo.api.identity.application.port.out.PlatformUserProfiles;

import jakarta.persistence.EntityManager;

/** Visible data of Kuador platform users, through the global persistence unit. */
public final class JpaPlatformUserProfiles implements PlatformUserProfiles, PlatformUserNames {

    private static final String BY_IDS = "SELECT u FROM PlatformUserEntity u WHERE u.id IN :ids";

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaPlatformUserProfiles(EntityManager global) {
        this.global = global;
    }

    @Override
    public Optional<String> displayNameOf(UUID id) {
        return Optional.ofNullable(global.find(PlatformUserEntity.class, id)).map(PlatformUserEntity::displayName);
    }

    @Override
    public Map<UUID, String> displayNamesOf(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return global.createQuery(BY_IDS, PlatformUserEntity.class)
                .setParameter("ids", ids.stream().distinct().toList())
                .getResultList().stream()
                .collect(Collectors.toMap(PlatformUserEntity::id, PlatformUserEntity::displayName));
    }
}
