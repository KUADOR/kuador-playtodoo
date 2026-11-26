package com.kuador.platodo.api.subscriptions.domain;

import java.util.List;

/** Currencies and periodicities a plan may use, from the platform parameters ([POR DEFINIR: values]). */
public record PlanOptions(List<String> currencies, List<String> periodicities) {

    public PlanOptions {
        currencies = List.copyOf(currencies);
        periodicities = List.copyOf(periodicities);
    }

    public boolean allowsCurrency(String currency) {
        return currencies.contains(currency);
    }

    public boolean allowsPeriodicity(String periodicity) {
        return periodicities.contains(periodicity);
    }
}
