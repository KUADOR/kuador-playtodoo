package com.kuador.platodo.api.identity.infrastructure.web;

import java.time.Clock;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.kuador.platodo.api.identity.application.AuthenticateRequest;
import com.kuador.platodo.api.identity.application.LogIn;
import com.kuador.platodo.api.identity.application.port.out.AccessTokens;
import com.kuador.platodo.api.identity.application.port.out.AccountDirectory;
import com.kuador.platodo.api.identity.application.port.out.PasswordHashes;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.identity.infrastructure.crypto.DelegatingPasswordHashes;
import com.kuador.platodo.api.identity.infrastructure.persistence.global.JpaPlatformUsers;
import com.kuador.platodo.api.identity.infrastructure.persistence.global.JpaPlayerAccounts;
import com.kuador.platodo.api.identity.infrastructure.persistence.tenant.JdbcTenantOwnerAccounts;
import com.kuador.platodo.api.identity.infrastructure.persistence.tenant.JpaStaffUsers;
import com.kuador.platodo.api.identity.infrastructure.token.AccessTokenProperties;
import com.kuador.platodo.api.identity.infrastructure.token.JwtAccessTokens;
import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerAccounts;

import jakarta.persistence.EntityManagerFactory;

/**
 * Identity and access (PLATODO-26 design): staff accounts in the schema of each complex, player and Kuador accounts
 * in the global schema, signed access tokens and the filter that accepts them per host.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AccessTokenProperties.class)
class IdentityConfiguration {

    static final String STAFF = "staff";
    static final String PLAYER = "player";
    static final String PLATFORM = "platform";

    /**
     * Right after {@code TenantResolutionFilter} ({@code HIGHEST_PRECEDENCE + 10}), so tokens are checked inside the
     * scope of the resolved complex and before any handler.
     */
    static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

    private static final String API_URL_PATTERN = "/api/*";
    private static final String HANDLER_EXCEPTION_RESOLVER = "handlerExceptionResolver";
    private static final String GLOBAL_ENTITY_MANAGER_FACTORY = "globalEntityManagerFactory";
    private static final String TENANT_ENTITY_MANAGER_FACTORY = "tenantEntityManagerFactory";

    @Bean
    AccessTokens accessTokens(AccessTokenProperties properties, ObjectProvider<Clock> clock) {
        return new JwtAccessTokens(
                properties.hmacKey(), properties.timeToLive(), clock.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    PasswordHashes passwordHashes() {
        return new DelegatingPasswordHashes();
    }

    @Bean
    @Qualifier(STAFF)
    AccountDirectory staffUsers(@Qualifier(TENANT_ENTITY_MANAGER_FACTORY) EntityManagerFactory tenant) {
        return new JpaStaffUsers(SharedEntityManagerCreator.createSharedEntityManager(tenant));
    }

    /** Owner accounts that Tenancy provisions in the schema of each new complex. */
    @Bean
    TenantOwnerAccounts tenantOwnerAccounts(DataSource tenantData, PasswordHashes passwords,
            ObjectProvider<Clock> clock) {
        return new JdbcTenantOwnerAccounts(JdbcClient.create(tenantData), passwords,
                clock.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    @Qualifier(PLAYER)
    AccountDirectory playerAccounts(@Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global) {
        return new JpaPlayerAccounts(SharedEntityManagerCreator.createSharedEntityManager(global));
    }

    @Bean
    @Qualifier(PLATFORM)
    AccountDirectory platformUsers(@Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global) {
        return new JpaPlatformUsers(SharedEntityManagerCreator.createSharedEntityManager(global));
    }

    @Bean
    @Qualifier(STAFF)
    LogIn staffLogIn(@Qualifier(STAFF) AccountDirectory staff, PasswordHashes passwords, AccessTokens tokens) {
        return new LogIn(AccountType.STAFF, staff, passwords, tokens);
    }

    @Bean
    @Qualifier(PLAYER)
    LogIn playerLogIn(@Qualifier(PLAYER) AccountDirectory players, PasswordHashes passwords, AccessTokens tokens) {
        return new LogIn(AccountType.PLAYER, players, passwords, tokens);
    }

    @Bean
    @Qualifier(PLATFORM)
    LogIn platformLogIn(@Qualifier(PLATFORM) AccountDirectory platformUsers, PasswordHashes passwords,
            AccessTokens tokens) {
        return new LogIn(AccountType.PLATFORM, platformUsers, passwords, tokens);
    }

    @Bean
    AuthenticateRequest authenticateRequest(AccessTokens tokens, @Qualifier(STAFF) AccountDirectory staff,
            @Qualifier(PLAYER) AccountDirectory players, @Qualifier(PLATFORM) AccountDirectory platformUsers) {
        return new AuthenticateRequest(tokens, staff, players, platformUsers);
    }

    @Bean
    FilterRegistrationBean<AccessTokenFilter> accessTokenFilter(AuthenticateRequest authenticateRequest,
            @Qualifier(HANDLER_EXCEPTION_RESOLVER) HandlerExceptionResolver problemRenderer) {
        FilterRegistrationBean<AccessTokenFilter> registration =
                new FilterRegistrationBean<>(new AccessTokenFilter(authenticateRequest, problemRenderer));
        registration.addUrlPatterns(API_URL_PATTERN);
        registration.setOrder(FILTER_ORDER);
        return registration;
    }
}
