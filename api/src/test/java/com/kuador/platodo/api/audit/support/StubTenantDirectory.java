package com.kuador.platodo.api.audit.support;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/** Fixed directory of complexes for application tests. */
public final class StubTenantDirectory implements TenantDirectory {

    private final Map<TenantId, TenantEntry> entries = new ConcurrentHashMap<>();

    public void add(TenantId tenant, TenantStatus status, String host) {
        entries.put(tenant, new TenantEntry(tenant, status, Optional.ofNullable(host)));
    }

    @Override
    public Optional<TenantEntry> find(TenantId tenant) {
        return Optional.ofNullable(entries.get(tenant));
    }
}
