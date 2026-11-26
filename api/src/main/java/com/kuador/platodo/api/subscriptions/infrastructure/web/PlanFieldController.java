package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.subscriptions.application.CreatePlanField;
import com.kuador.platodo.api.subscriptions.application.ListPlanFields;
import com.kuador.platodo.api.subscriptions.application.RetirePlanField;
import com.kuador.platodo.api.subscriptions.application.UpdatePlanField;

/** Tag «Plan fields» of {@code contracts/platform-backoffice.openapi.yaml}; global host. */
@RestController
@RequestMapping(PlansApiPaths.PLATFORM)
class PlanFieldController {

    private final ListPlanFields listFields;
    private final CreatePlanField createField;
    private final UpdatePlanField updateField;
    private final RetirePlanField retireField;

    PlanFieldController(ListPlanFields listFields, CreatePlanField createField, UpdatePlanField updateField,
            RetirePlanField retireField) {
        this.listFields = listFields;
        this.createField = createField;
        this.updateField = updateField;
        this.retireField = retireField;
    }

    @GetMapping(PlansApiPaths.PLAN_FIELDS)
    List<PlanFieldResponse> list() {
        return listFields.list().stream().map(PlanFieldResponse::of).toList();
    }

    @PostMapping(PlansApiPaths.PLAN_FIELDS)
    @ResponseStatus(HttpStatus.CREATED)
    PlanFieldResponse create(@RequestBody CreatePlanFieldRequest request) {
        return PlanFieldResponse.of(createField.create(request.label(), request.requiredType(),
                request.requiredFlag()));
    }

    @PutMapping(PlansApiPaths.PLAN_FIELD)
    PlanFieldResponse update(@PathVariable UUID fieldId, @RequestBody UpdatePlanFieldRequest request) {
        return PlanFieldResponse.of(updateField.update(fieldId, request.requiredVersion(), request.label(),
                request.requiredFlag()));
    }

    @PostMapping(PlansApiPaths.PLAN_FIELD_RETIREMENT)
    PlanFieldResponse retire(@PathVariable UUID fieldId) {
        return PlanFieldResponse.of(retireField.retire(fieldId));
    }
}
