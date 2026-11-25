package com.kuador.platodo.api.tenancy.domain.event;

import java.time.Instant;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** The complex was cancelled: it is {@code ARCHIVED} and may be purged from {@code purgeDueAt} on. */
public record TenantArchived(TenantId tenantId, Instant purgeDueAt, Instant occurredAt) implements TenantEvent {
}
