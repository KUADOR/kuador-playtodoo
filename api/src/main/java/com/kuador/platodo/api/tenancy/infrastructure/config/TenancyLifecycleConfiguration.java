package com.kuador.platodo.api.tenancy.infrastructure.config;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kuador.platodo.api.tenancy.application.PurgeDueTenants;
import com.kuador.platodo.api.tenancy.application.RequestHosts;
import com.kuador.platodo.api.tenancy.application.TenancyUseCases;
import com.kuador.platodo.api.tenancy.application.port.in.TenantOnboarding;
import com.kuador.platodo.api.tenancy.application.port.in.TenantRetention;
import com.kuador.platodo.api.tenancy.application.port.out.GlobalTransaction;
import com.kuador.platodo.api.tenancy.application.port.out.SubdomainUsage;
import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerAccounts;
import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerNotifier;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.application.port.out.TenantSchemas;
import com.kuador.platodo.api.tenancy.domain.SubdomainRules;
import com.kuador.platodo.api.tenancy.infrastructure.events.SpringTenantEventPublisher;
import com.kuador.platodo.api.tenancy.infrastructure.events.SubscriptionStatusEffects;
import com.kuador.platodo.api.tenancy.infrastructure.identity.PrincipalCallers;

/** Tenant lifecycle, purge and subdomain use cases (ADR-012, ADR-013, ADR-014). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TenancyProperties.class)
class TenancyLifecycleConfiguration {

    /**
     * The configured reserved list plus the label of the global host, which can never be a complex whatever the
     * configuration says.
     */
    @Bean
    SubdomainRules subdomainRules(TenancyProperties properties) {
        List<String> reserved = new ArrayList<>(properties.subdomains().reserved());
        reserved.add(RequestHosts.GLOBAL_LABEL);
        return new SubdomainRules(reserved);
    }

    @Bean
    TenancyUseCases tenancyUseCases(TenancyProperties properties, SubdomainRules rules, TenantRegistry registry,
            SubdomainUsage subdomainUsage, TenantSchemas schemas, TenantOwnerAccounts owners,
            GlobalTransaction transaction, ApplicationEventPublisher publisher, ObjectProvider<Clock> clock) {
        TenancyUseCases.Ports ports = new TenancyUseCases.Ports(new PrincipalCallers(), registry, subdomainUsage,
                schemas, owners, new SpringTenantEventPublisher(publisher), transaction);
        return TenancyUseCases.create(ports, rules, clock.getIfAvailable(Clock::systemUTC),
                properties.archive().defaultRetentionDays());
    }

    /** Registration of complexes for Plans and subscriptions (ADR-014). */
    @Bean
    TenantOnboarding tenantOnboarding(TenancyUseCases useCases) {
        return useCases.onboarding();
    }

    /** Retention changes of a suspended or archived complex for Plans and subscriptions (ADR-014). */
    @Bean
    TenantRetention tenantRetention(TenancyUseCases useCases) {
        return useCases.lifecycle();
    }

    /** Effect of a subscription status change on the tenant, inside the transaction of the change (ADR-014). */
    @Bean
    SubscriptionStatusEffects subscriptionStatusEffects(TenancyUseCases useCases) {
        return new SubscriptionStatusEffects(useCases.lifecycle());
    }

    @Bean
    PurgeDueTenants purgeDueTenants(TenantRegistry registry, TenantSchemas schemas, TenantOwnerAccounts owners,
            TenantOwnerNotifier notifier, GlobalTransaction transaction, ApplicationEventPublisher publisher,
            ObjectProvider<Clock> clock) {
        PurgeDueTenants.Ports ports = new PurgeDueTenants.Ports(registry, schemas, owners, notifier,
                new SpringTenantEventPublisher(publisher), transaction);
        return new PurgeDueTenants(ports, clock.getIfAvailable(Clock::systemUTC));
    }
}
