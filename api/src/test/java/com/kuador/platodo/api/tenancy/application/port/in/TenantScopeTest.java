package com.kuador.platodo.api.tenancy.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class TenantScopeTest {

    private static final TenantId TENANT_A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final TenantId TENANT_B = new TenantId("0b0b0b0b-0000-4000-8000-00000000000b");

    @Test
    void withoutAnOpenScopeRequireFailsClosed() {
        assertThat(TenantScope.current()).isEmpty();
        assertThatThrownBy(TenantScope::require).isInstanceOf(NoTenantInScopeException.class);
    }

    @Test
    void insideAScopeTheTenantIsAvailableAndItEndsWithTheOperation() {
        TenantId seen = TenantScope.call(TENANT_A, TenantScope::require);

        assertThat(seen).isEqualTo(TENANT_A);
        assertThat(TenantScope.current()).isEmpty();
    }

    @Test
    void theScopeEndsEvenWhenTheOperationFails() {
        assertThatThrownBy(() -> TenantScope.run(TENANT_A, () -> {
            throw new IllegalArgumentException("boom");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(TenantScope.current()).isEmpty();
    }

    @Test
    void reopeningTheSameTenantIsAllowed() {
        TenantId seen = TenantScope.call(TENANT_A, () -> TenantScope.call(TENANT_A, TenantScope::require));

        assertThat(seen).isEqualTo(TENANT_A);
    }

    @Test
    void anotherTenantCannotBeOpenedInsideAnOpenScope() {
        assertThatThrownBy(() -> TenantScope.run(TENANT_A, () -> TenantScope.run(TENANT_B, () -> { })))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theScopeIsNotInheritedByAnotherThread() throws InterruptedException {
        AtomicReference<Optional<TenantId>> seenByOtherThread = new AtomicReference<>();

        TenantScope.call(TENANT_A, () -> {
            Thread other = Thread.ofVirtual().start(() -> seenByOtherThread.set(TenantScope.current()));
            other.join();
            return null;
        });

        assertThat(seenByOtherThread.get()).isEmpty();
    }
}
