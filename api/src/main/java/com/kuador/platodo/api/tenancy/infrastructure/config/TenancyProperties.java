package com.kuador.platodo.api.tenancy.infrastructure.config;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.kuador.platodo.api.tenancy.domain.Tenant;

/**
 * Settings of the tenant lifecycle and subdomains ({@code tenancy.*}, ADR-012 and ADR-013).
 *
 * @param archive    {@code tenancy.archive.*}
 * @param subdomains {@code tenancy.subdomains.*}
 */
@ConfigurationProperties("tenancy")
public record TenancyProperties(Archive archive, Subdomains subdomains) {

    public TenancyProperties {
        Objects.requireNonNull(archive, "tenancy.archive is required");
        Objects.requireNonNull(subdomains, "tenancy.subdomains is required");
    }

    /** @param defaultRetentionDays retention of a new archive, in days ({@code 45} by decision of Kuador) */
    public record Archive(int defaultRetentionDays) {

        public Archive {
            if (defaultRetentionDays < Tenant.MIN_RETENTION_DAYS) {
                throw new IllegalArgumentException("tenancy.archive.default-retention-days must be at least "
                        + Tenant.MIN_RETENTION_DAYS);
            }
        }
    }

    /** @param reserved subdomains no complex may take */
    public record Subdomains(List<String> reserved) {

        public Subdomains {
            reserved = List.copyOf(Objects.requireNonNull(reserved, "tenancy.subdomains.reserved is required"));
        }
    }
}
