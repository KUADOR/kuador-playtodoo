package com.kuador.platodo.api.identity.application.port.out;

import java.util.List;

import com.kuador.platodo.api.identity.domain.CapabilityToggle;
import com.kuador.platodo.api.identity.domain.OperatorCapabilitySettings;

/** Write side of the capabilities enabled for operators. Both methods need a transaction of the global schema. */
public interface OperatorCapabilityUpdates {

    /** The current settings, locked until the transaction ends so that concurrent changes run one after the other. */
    OperatorCapabilitySettings lockCurrent();

    /** Stores {@code changes} and makes {@code newVersion} the version of the settings. */
    void apply(List<CapabilityToggle> changes, long newVersion);
}
