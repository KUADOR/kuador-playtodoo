package com.kuador.platodo.api.notifications.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

import com.kuador.platodo.api.notifications.infrastructure.persistence.global.OutboxTenantOwnerNotifier;
import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerNotifier;

import jakarta.persistence.EntityManagerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Notifications (PLATODO-26 design): for now only the outbox behind the ports of other modules. Sending arrives with
 * the notifications epic, once its e-mail provider is chosen ([POR DEFINIR]).
 */
@Configuration(proxyBeanMethods = false)
class NotificationsConfiguration {

    private static final String GLOBAL_ENTITY_MANAGER_FACTORY = "globalEntityManagerFactory";

    @Bean
    TenantOwnerNotifier tenantOwnerNotifier(@Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global) {
        return new OutboxTenantOwnerNotifier(SharedEntityManagerCreator.createSharedEntityManager(global),
                JsonMapper.builder().build());
    }
}
