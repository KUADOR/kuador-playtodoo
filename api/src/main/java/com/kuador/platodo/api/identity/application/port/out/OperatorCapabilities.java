package com.kuador.platodo.api.identity.application.port.out;

import com.kuador.platodo.api.identity.domain.OperatorCapabilitySettings;

/** Read side of the capabilities enabled for operators, in the global schema. */
@FunctionalInterface
public interface OperatorCapabilities {

    /** The settings as committed now; capabilities of the code without a stored row are disabled. */
    OperatorCapabilitySettings current();
}
