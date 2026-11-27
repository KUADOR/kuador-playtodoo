package com.kuador.platodo.api.tenancy.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.application.port.out.GlobalTransaction;
import com.kuador.platodo.api.tenancy.application.port.out.TenantEventPublisher;
import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerAccounts;
import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerNotifier;
import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerNotifier.PermanentDeletionNotice;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry.RegisteredTenant;
import com.kuador.platodo.api.tenancy.application.port.out.TenantSchemas;
import com.kuador.platodo.api.tenancy.domain.Tenant;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;
import com.kuador.platodo.api.tenancy.domain.event.TenantPurged;

/**
 * Permanent deletion of the archived complexes whose retention ended (ADR-013, D8), safe with several api instances.
 * <ol>
 *   <li>{@link #claimDue()} moves the due {@code ARCHIVED} complexes to {@code PURGING} in one transaction, skipping
 *       the rows another instance holds: from then on they cannot be reactivated and their host answers "not found".
 *   </li>
 *   <li>{@link #purge(TenantId)} purges one {@code PURGING} complex in one transaction that holds its row: queues the
 *       e-mail to the owner, drops the schema, marks it {@code PURGED}, retires its subdomain, redacts the owner
 *       e-mail of the global registry (ADR-016) and publishes
 *       {@link TenantPurged}, whose consumers anonymize their own global data within the same transaction (D11).</li>
 * </ol>
 * If anything fails, the whole purge of that complex rolls back (PostgreSQL drops schemas transactionally): it stays
 * {@code PURGING} with its schema, no e-mail is queued, and the next run retries it from the start. Each run thus
 * queues the e-mail exactly once and never leaves a {@code PURGED} complex with personal data.
 */
public final class PurgeDueTenants {

    /** The adapters the purge needs. */
    public record Ports(
            TenantRegistry registry,
            TenantSchemas schemas,
            TenantOwnerAccounts owners,
            TenantOwnerNotifier notifier,
            TenantEventPublisher events,
            GlobalTransaction transaction) {
    }

    private final Ports ports;
    private final Clock clock;

    /** @param clock any clock; it is read at the precision of the database, like the other lifecycle use cases */
    public PurgeDueTenants(Ports ports, Clock clock) {
        this.ports = ports;
        this.clock = TenancyUseCases.atDatabasePrecision(clock);
    }

    /** Moves every {@code ARCHIVED} complex whose purge date was reached to {@code PURGING}; returns how many. */
    public int claimDue() {
        Instant now = clock.instant();
        return ports.transaction().run(() -> {
            List<RegisteredTenant> due = ports.registry().lockArchivedDueBy(now);
            due.forEach(claimed -> ports.registry().save(claimed.tenant().startPurge(now)));
            return due.size();
        });
    }

    /** The complexes whose purge is pending: claimed by this run or left by a failed one. */
    public List<TenantId> pending() {
        return ports.registry().purging();
    }

    /**
     * Purges one {@code PURGING} complex.
     *
     * @return false when there was nothing to do: another instance holds it, or it is no longer {@code PURGING}
     */
    public boolean purge(TenantId id) {
        return ports.transaction().run(() -> ports.registry().tryLock(id)
                .filter(locked -> locked.tenant().is(TenantStatus.PURGING))
                .map(locked -> purgeLocked(locked.tenant()))
                .orElse(false));
    }

    private boolean purgeLocked(Tenant purging) {
        Instant now = clock.instant();
        TenantId id = purging.id();
        notifyOwner(purging, now);
        ports.schemas().dropIfExists(id);
        ports.registry().save(purging.markPurged(now));
        ports.registry().retireCurrentSubdomain(id, now);
        ports.registry().redactOwnerEmail(id);
        ports.events().publish(new TenantPurged(id, now));
        return true;
    }

    /**
     * The owner account lives in the schema, so a complex whose schema is already gone (dropped by hand) or that has
     * no owner account has nobody to tell; its purge goes on.
     */
    private void notifyOwner(Tenant purging, Instant purgedAt) {
        if (!ports.schemas().exists(purging.id())) {
            return;
        }
        Optional<String> ownerEmail = TenantScope.call(purging.id(), ports.owners()::ownerEmail);
        ownerEmail.ifPresent(email -> ports.notifier().notifyPermanentDeletion(
                new PermanentDeletionNotice(purging.id(), purging.displayName(), email, purgedAt)));
    }
}
