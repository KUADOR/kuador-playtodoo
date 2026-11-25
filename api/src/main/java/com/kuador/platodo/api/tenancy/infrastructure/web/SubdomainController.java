package com.kuador.platodo.api.tenancy.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.tenancy.application.CheckSubdomainAvailability;
import com.kuador.platodo.api.tenancy.application.SuggestSubdomain;
import com.kuador.platodo.api.tenancy.application.TenancyUseCases;
import com.kuador.platodo.api.tenancy.domain.Tenant;

/** Tag «Subdomains» of {@code contracts/tenancy.openapi.yaml}: public, global host. */
@RestController
@RequestMapping(TenancyApiPaths.SUBDOMAINS)
class SubdomainController {

    private final SuggestSubdomain suggestSubdomain;
    private final CheckSubdomainAvailability checkAvailability;

    SubdomainController(TenancyUseCases useCases) {
        this.suggestSubdomain = useCases.suggestSubdomain();
        this.checkAvailability = useCases.checkSubdomainAvailability();
    }

    @GetMapping("/suggestion")
    SubdomainResponses.Suggestion suggest(@RequestParam String name) {
        RequestFields.requireText(name, "name", Tenant.MAX_DISPLAY_NAME_LENGTH);
        return new SubdomainResponses.Suggestion(suggestSubdomain.suggest(name).value());
    }

    @GetMapping("/{subdomain}/availability")
    SubdomainResponses.AvailabilityResponse availability(@PathVariable String subdomain) {
        RequestFields.requireSubdomainCandidate(subdomain);
        return SubdomainResponses.AvailabilityResponse.of(checkAvailability.check(subdomain));
    }
}
