package com.kuador.platodo.api.identity.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.identity.application.ChangeOperatorCapabilities;
import com.kuador.platodo.api.identity.application.GetCurrentPlatformUser;
import com.kuador.platodo.api.identity.application.ListCapabilityChanges;
import com.kuador.platodo.api.identity.application.ListOperatorCapabilities;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog.PageRequest;

/** Tags «Me» and «Operator capabilities» of {@code contracts/platform-backoffice.openapi.yaml}; global host. */
@RestController
@RequestMapping(PlatformApiPaths.PLATFORM)
class PlatformIdentityController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final GetCurrentPlatformUser currentUser;
    private final ListOperatorCapabilities listCapabilities;
    private final ChangeOperatorCapabilities changeCapabilities;
    private final ListCapabilityChanges listChanges;

    PlatformIdentityController(GetCurrentPlatformUser currentUser, ListOperatorCapabilities listCapabilities,
            ChangeOperatorCapabilities changeCapabilities, ListCapabilityChanges listChanges) {
        this.currentUser = currentUser;
        this.listCapabilities = listCapabilities;
        this.changeCapabilities = changeCapabilities;
        this.listChanges = listChanges;
    }

    @GetMapping(PlatformApiPaths.ME)
    CurrentPlatformUserResponse me() {
        return CurrentPlatformUserResponse.of(currentUser.get());
    }

    @GetMapping(PlatformApiPaths.OPERATOR_CAPABILITIES)
    OperatorCapabilitiesResponse operatorCapabilities() {
        return OperatorCapabilitiesResponse.of(listCapabilities.list());
    }

    @PutMapping(PlatformApiPaths.OPERATOR_CAPABILITIES)
    OperatorCapabilitiesResponse changeOperatorCapabilities(@RequestBody ChangeOperatorCapabilitiesRequest request) {
        return OperatorCapabilitiesResponse.of(
                changeCapabilities.change(request.requiredVersion(), request.toggles()));
    }

    @GetMapping(PlatformApiPaths.OPERATOR_CAPABILITY_CHANGES)
    CapabilityChangePageResponse operatorCapabilityChanges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPlatformRequestException(
                    "`page` must be >= 0 and `size` between 1 and %d.".formatted(MAX_PAGE_SIZE));
        }
        PageRequest pageRequest = new PageRequest(page, size);
        return CapabilityChangePageResponse.of(listChanges.list(pageRequest), pageRequest);
    }
}
