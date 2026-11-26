package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.subscriptions.application.CreatePlan;
import com.kuador.platodo.api.subscriptions.application.GetPlan;
import com.kuador.platodo.api.subscriptions.application.GetPlanOptions;
import com.kuador.platodo.api.subscriptions.application.ListPlans;
import com.kuador.platodo.api.subscriptions.application.PublishPlan;
import com.kuador.platodo.api.subscriptions.application.UpdatePlan;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.domain.Plan;
import com.kuador.platodo.api.subscriptions.domain.PlanStatus;

/** Tag «Plans» of {@code contracts/platform-backoffice.openapi.yaml}; global host. */
@RestController
@RequestMapping(PlansApiPaths.PLATFORM)
class PlanController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ListPlans listPlans;
    private final GetPlan getPlan;
    private final CreatePlan createPlan;
    private final UpdatePlan updatePlan;
    private final PublishPlan publishPlan;
    private final GetPlanOptions getPlanOptions;

    PlanController(ListPlans listPlans, GetPlan getPlan, CreatePlan createPlan, UpdatePlan updatePlan,
            PublishPlan publishPlan, GetPlanOptions getPlanOptions) {
        this.listPlans = listPlans;
        this.getPlan = getPlan;
        this.createPlan = createPlan;
        this.updatePlan = updatePlan;
        this.publishPlan = publishPlan;
        this.getPlanOptions = getPlanOptions;
    }

    @GetMapping(PlansApiPaths.PLANS)
    PlanPageResponse list(@RequestParam(required = false) PlanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidSubscriptionsRequestException(
                    "`page` must be >= 0 and `size` between 1 and %d.".formatted(MAX_PAGE_SIZE));
        }
        PageRequest pageRequest = new PageRequest(page, size);
        return PlanPageResponse.of(listPlans.list(Optional.ofNullable(status), pageRequest), pageRequest);
    }

    @PostMapping(PlansApiPaths.PLANS)
    ResponseEntity<PlanResponse> create(@RequestBody PlanRequest request) {
        Plan plan = createPlan.create(request.toDraft());
        URI location = URI.create(PlansApiPaths.PLATFORM + PlansApiPaths.PLANS + "/" + plan.id());
        return ResponseEntity.created(location).body(PlanResponse.of(plan));
    }

    @GetMapping(PlansApiPaths.PLAN)
    PlanResponse get(@PathVariable UUID planId) {
        return PlanResponse.of(getPlan.get(planId));
    }

    @PutMapping(PlansApiPaths.PLAN)
    PlanResponse update(@PathVariable UUID planId, @RequestBody PlanRequest request) {
        return PlanResponse.of(updatePlan.update(planId, request.requiredVersion(), request.toDraft()));
    }

    @PostMapping(PlansApiPaths.PLAN_PUBLICATION)
    PlanResponse publish(@PathVariable UUID planId) {
        return PlanResponse.of(publishPlan.publish(planId));
    }

    @GetMapping(PlansApiPaths.PLAN_OPTIONS)
    PlanOptionsResponse options() {
        return PlanOptionsResponse.of(getPlanOptions.get());
    }
}
