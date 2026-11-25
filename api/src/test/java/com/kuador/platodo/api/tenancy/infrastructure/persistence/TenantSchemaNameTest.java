package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

class TenantSchemaNameTest {

    @Test
    void isTPrefixAndTheTenantUuidWithoutHyphens() {
        TenantSchemaName schema = TenantSchemaName.of(new TenantId("0a0a0a0a-0000-4000-8000-00000000000a"));

        assertThat(schema.value()).isEqualTo("t_0a0a0a0a00004000800000000000000a");
    }

    @ParameterizedTest
    @ValueSource(strings = {"tenant_a", "1-1-1-1-1", "0A0A0A0A-0000-4000-8000-00000000000A", "public"})
    void rejectsTenantIdsThatAreNotCanonicalUuids(String tenantId) {
        TenantId tenant = new TenantId(tenantId);

        assertThatIllegalArgumentException().isThrownBy(() -> TenantSchemaName.of(tenant));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "public", "global", "no_tenant", "extensions", "t_0A0A0A0A000040008000000000000000",
        "t_0a0a", "t_0a0a0a0a000040008000000000000000; DROP SCHEMA global", "\"t_0a0a0a0a000040008000000000000000\""})
    void rejectsAnythingThatIsNotATenantSchemaName(String value) {
        assertThatIllegalArgumentException().isThrownBy(() -> new TenantSchemaName(value));
    }
}
