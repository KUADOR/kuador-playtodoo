package com.kuador.platodo.api.tenancy.application.port.out;

import java.util.function.Supplier;

/** A transaction on the global schema: {@code work} commits as a whole, or nothing of it when it throws. */
@FunctionalInterface
public interface GlobalTransaction {

    <T> T run(Supplier<T> work);
}
