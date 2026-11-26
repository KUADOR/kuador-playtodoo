package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.subscriptions.application.ComplexDetail;
import com.kuador.platodo.api.subscriptions.application.GetComplex;
import com.kuador.platodo.api.subscriptions.application.ListComplexes;
import com.kuador.platodo.api.subscriptions.application.RegisterComplex;
import com.kuador.platodo.api.subscriptions.application.RetryComplexProvisioning;

/** Tag «Complexes» of {@code contracts/platform-backoffice.openapi.yaml}; global host. */
@RestController
@RequestMapping(PlansApiPaths.PLATFORM)
class ComplexController {

    static final String COMPLEXES = "/complexes";
    static final String COMPLEX = COMPLEXES + "/{complexId}";
    static final String PROVISIONING_RETRIES = COMPLEX + "/provisioning-retries";

    private final ListComplexes listComplexes;
    private final GetComplex getComplex;
    private final RegisterComplex registerComplex;
    private final RetryComplexProvisioning retryProvisioning;

    ComplexController(ListComplexes listComplexes, GetComplex getComplex, RegisterComplex registerComplex,
            RetryComplexProvisioning retryProvisioning) {
        this.listComplexes = listComplexes;
        this.getComplex = getComplex;
        this.registerComplex = registerComplex;
        this.retryProvisioning = retryProvisioning;
    }

    @GetMapping(COMPLEXES)
    ComplexResponses.Page list(@RequestParam(defaultValue = PageParameters.FIRST_PAGE) int page,
            @RequestParam(defaultValue = PageParameters.DEFAULT_SIZE) int size) {
        return ComplexResponses.Page.of(listComplexes.list(PageParameters.of(page, size)), page, size);
    }

    /** {@code 201} whether provisioning ended {@code ACTIVE} or {@code PROVISIONING_FAILED}. */
    @PostMapping(COMPLEXES)
    ResponseEntity<ComplexResponses.Detail> register(@RequestBody RegisterComplexRequest request) {
        ComplexDetail complex = registerComplex.register(request.toRegistration());
        URI location = URI.create(
                PlansApiPaths.PLATFORM + COMPLEXES + "/" + complex.subscription().tenantId());
        return ResponseEntity.created(location).body(ComplexResponses.Detail.of(complex));
    }

    @GetMapping(COMPLEX)
    ComplexResponses.Detail get(@PathVariable UUID complexId) {
        return ComplexResponses.Detail.of(getComplex.get(complexId));
    }

    @PostMapping(PROVISIONING_RETRIES)
    ResponseEntity<ComplexResponses.Detail> retryProvisioning(@PathVariable UUID complexId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ComplexResponses.Detail.of(retryProvisioning.retry(complexId)));
    }
}
