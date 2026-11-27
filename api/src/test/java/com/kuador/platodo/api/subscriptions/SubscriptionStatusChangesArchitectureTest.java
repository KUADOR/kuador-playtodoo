package com.kuador.platodo.api.subscriptions;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.subscriptions.application.ChangeComplexPlan;
import com.kuador.platodo.api.subscriptions.application.ChangeSubscriptionStatus;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * PLATODO-27 CA6 and ADR-014: no process changes the status of a subscription. A stored subscription changes only
 * through the two manual operations, and those are reached only from their HTTP endpoints, never from a scheduled
 * job, an event listener or another module.
 */
class SubscriptionStatusChangesArchitectureTest {

    private static final String BASE_PACKAGE = "com.kuador.platodo.api";
    private static final String WEB = BASE_PACKAGE + ".subscriptions.infrastructure.web..";
    private static final String CONFIG = BASE_PACKAGE + ".subscriptions.infrastructure.config..";

    private static JavaClasses production;

    @BeforeAll
    static void importProductionCode() {
        production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    void onlyTheManualOperationsUpdateASubscription() {
        noClasses()
                .that().doNotHaveFullyQualifiedName(ChangeSubscriptionStatus.class.getName())
                .and().doNotHaveFullyQualifiedName(ChangeComplexPlan.class.getName())
                .should().callMethod(Subscriptions.class, "update", Subscription.class)
                .check(production);
    }

    @Test
    void theManualOperationsAreReachedOnlyFromTheirEndpoints() {
        noClasses()
                .that().resideOutsideOfPackages(WEB, CONFIG)
                .and().doNotHaveFullyQualifiedName(ChangeSubscriptionStatus.class.getName())
                .and().doNotHaveFullyQualifiedName(ChangeComplexPlan.class.getName())
                .should().dependOnClassesThat().areAssignableTo(ChangeSubscriptionStatus.class)
                .orShould().dependOnClassesThat().areAssignableTo(ChangeComplexPlan.class)
                .check(production);
    }
}
