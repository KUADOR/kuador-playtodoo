package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.util.function.Supplier;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.kuador.platodo.api.tenancy.application.port.out.GlobalTransaction;

/**
 * Runs work in a transaction of the global transaction manager, which also binds the pooled JDBC connection, so
 * the JDBC adapters of the global schema join it.
 */
public final class SpringGlobalTransaction implements GlobalTransaction {

    private final TransactionTemplate template;

    public SpringGlobalTransaction(PlatformTransactionManager globalTransactions) {
        this.template = new TransactionTemplate(globalTransactions);
    }

    @Override
    public <T> T run(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
