package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;

import com.kuador.platodo.api.subscriptions.application.PlanNameTakenException;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.domain.Plan;
import com.kuador.platodo.api.subscriptions.domain.PlanContent;
import com.kuador.platodo.api.subscriptions.domain.PlanStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plans through the global persistence unit. Name uniqueness is enforced by the unique index on {@code lower(name)},
 * so two concurrent requests with the same name never both succeed.
 */
public final class JpaPlans implements Plans {

    private static final String NAME_UNIQUE_INDEX = "plan_name_unique";
    private static final String ALL = "SELECT p FROM PlanEntity p ORDER BY lower(p.name), p.id";
    private static final String COUNT_ALL = "SELECT count(p) FROM PlanEntity p";
    private static final String BY_STATUS =
            "SELECT p FROM PlanEntity p WHERE p.status = :status ORDER BY lower(p.name), p.id";
    private static final String COUNT_BY_STATUS = "SELECT count(p) FROM PlanEntity p WHERE p.status = :status";
    private static final String STATUS = "status";

    private final EntityManager global;
    private final CustomFieldsJson customFields;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaPlans(EntityManager global, JsonMapper json) {
        this.global = global;
        this.customFields = new CustomFieldsJson(json);
    }

    @Override
    public Optional<Plan> find(UUID id) {
        return Optional.ofNullable(global.find(PlanEntity.class, id)).map(this::toPlan);
    }

    @Override
    public Optional<Plan> lock(UUID id) {
        return Optional.ofNullable(global.find(PlanEntity.class, id, LockModeType.PESSIMISTIC_WRITE))
                .map(this::toPlan);
    }

    @Override
    public PlanPage page(Optional<PlanStatus> status, PageRequest page) {
        TypedQuery<PlanEntity> items = status.isPresent()
                ? global.createQuery(BY_STATUS, PlanEntity.class).setParameter(STATUS, status.get().name())
                : global.createQuery(ALL, PlanEntity.class);
        TypedQuery<Long> count = status.isPresent()
                ? global.createQuery(COUNT_BY_STATUS, Long.class).setParameter(STATUS, status.get().name())
                : global.createQuery(COUNT_ALL, Long.class);
        List<Plan> plans = items.setFirstResult(Math.toIntExact(page.offset()))
                .setMaxResults(page.size())
                .getResultList().stream()
                .map(this::toPlan)
                .toList();
        return new PlanPage(plans, count.getSingleResult());
    }

    @Override
    public void insert(Plan plan) {
        PlanEntity row = new PlanEntity(plan.id());
        fill(row, plan);
        global.persist(row);
        flushTranslatingNameTaken();
    }

    @Override
    public void update(Plan plan) {
        PlanEntity row = global.find(PlanEntity.class, plan.id());
        if (row == null) {
            throw new IllegalStateException("Plan " + plan.id() + " does not exist");
        }
        fill(row, plan);
        flushTranslatingNameTaken();
    }

    private void flushTranslatingNameTaken() {
        try {
            global.flush();
        } catch (ConstraintViolationException violation) {
            if (NAME_UNIQUE_INDEX.equals(violation.getConstraintName())) {
                throw new PlanNameTakenException();
            }
            throw violation;
        }
    }

    private void fill(PlanEntity row, Plan plan) {
        PlanContent content = plan.content();
        row.fill(content.name(), content.price(), content.currency(), content.periodicity(), content.courtLimit(),
                content.userLimit(), customFields.write(content.customFields()), plan.status().name(),
                plan.version());
    }

    private Plan toPlan(PlanEntity row) {
        PlanContent content = new PlanContent(row.name(), row.price(), row.currency(), row.periodicity(),
                row.courtLimit(), row.userLimit(), customFields.read(row.customFields()));
        return new Plan(row.id(), content, PlanStatus.valueOf(row.status()), row.version());
    }
}
