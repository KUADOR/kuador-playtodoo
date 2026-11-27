import {
  CreatePlanFieldRequest,
  PlanField,
  UpdatePlanFieldRequest,
} from '../../features/plans/plan-fields-api';
import {
  CustomFieldValues,
  Plan,
  PlanFields,
  PlanOptions,
  UpdatePlanRequest,
} from '../../features/plans/plans-api';
import {
  MockFieldError,
  MockRequest,
  MockResponse,
  created,
  ok,
  pageOf,
  problem,
} from './mock-responses';

/**
 * In-memory stand-in of the «Plans» and «Plan fields» operations of
 * contracts/platform-backoffice.openapi.yaml, seeded from its examples. Development builds only.
 * Every operation is SUPER_ADMIN only, except listing and reading published plans.
 */
const CUSTOM_FIELDS_PREFIX = 'customFields.';

/** Illustrative values of `GET /plan-options`: the real ones are [POR DEFINIR]. */
const PLAN_OPTIONS: PlanOptions = { currencies: ['USD'], periodicities: ['MONTHLY', 'YEARLY'] };

const BASIC_FIELDS: readonly PlanField[] = [
  basicField('name', 'Name', 'TEXT'),
  basicField('price', 'Price', 'NUMBER'),
  basicField('currency', 'Currency', 'TEXT'),
  basicField('periodicity', 'Periodicity', 'TEXT'),
  basicField('courtLimit', 'Court limit', 'NUMBER'),
  basicField('userLimit', 'User limit', 'NUMBER'),
];
const BASIC_REQUIRED_TO_PUBLISH: readonly (keyof PlanFields)[] = [
  'price',
  'currency',
  'periodicity',
  'courtLimit',
  'userLimit',
];

function basicField(key: string, label: string, type: PlanField['type']): PlanField {
  return {
    id: `mock-basic-${key}`,
    key,
    label,
    type,
    required: true,
    origin: 'BASIC',
    retiredAt: null,
    version: 0,
  };
}

const MOCK_FIELD_TYPE_CHECKS: Readonly<Record<PlanField['type'], (value: unknown) => boolean>> = {
  TEXT: (value) => typeof value === 'string',
  NUMBER: (value) => typeof value === 'number',
  BOOLEAN: (value) => typeof value === 'boolean',
};

/** `GET `, `PUT :id`, `POST :id/publication`… : the method and the shape of the path. */
function routeOf(method: string, id: string | undefined, subresource: string | undefined): string {
  return `${method} ${id === undefined ? '' : ':id'}${subresource ? `/${subresource}` : ''}`;
}

/** `key` of a new custom field: derived from its label, lower case with hyphens. */
function keyOf(label: string): string {
  return label
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

export class MockPlansBackend {
  private readonly plans: Plan[] = [
    {
      id: '2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e',
      name: 'Premium',
      price: '120.00',
      currency: 'USD',
      periodicity: 'MONTHLY',
      courtLimit: 10,
      userLimit: 25,
      customFields: { 'support-hours': 'Lunes a viernes de 9 a 18' },
      status: 'PUBLISHED',
      version: 2,
    },
    {
      id: '3c4d5e6f-7a8b-4c9d-8e0f-1a2b3c4d5e6f',
      name: 'Inicial',
      price: null,
      currency: null,
      periodicity: null,
      courtLimit: null,
      userLimit: null,
      customFields: {},
      status: 'DRAFT',
      version: 0,
    },
  ];
  private readonly fields: PlanField[] = [
    ...BASIC_FIELDS,
    {
      id: '9f0a1b2c-3d4e-4f5a-8b6c-7d8e9f0a1b2c',
      key: 'support-hours',
      label: 'Horario de soporte',
      type: 'TEXT',
      required: false,
      origin: 'CUSTOM',
      retiredAt: null,
      version: 0,
    },
  ];
  private nextId = 1;

  /** Answers a plans or plan fields operation; null when `request` is not one of them. */
  handle(request: MockRequest, isSuperAdmin: boolean): MockResponse | null {
    const [, , resource, id, subresource] = request.path.split('/');
    if (resource === 'plan-options' && request.method === 'GET') {
      return ok(PLAN_OPTIONS);
    }
    if (resource === 'plans') {
      return this.handlePlans(request, isSuperAdmin, id, subresource);
    }
    if (resource === 'plan-fields') {
      return isSuperAdmin ? this.handleFields(request, id, subresource) : forbidden();
    }
    return null;
  }

  private handlePlans(
    request: MockRequest,
    isSuperAdmin: boolean,
    id: string | undefined,
    subresource: string | undefined,
  ): MockResponse | null {
    const route = routeOf(request.method, id, subresource);
    if (route === 'GET ') {
      return this.listPlans(request.query, isSuperAdmin);
    }
    if (route === 'GET :id') {
      const plan = this.plan(id);
      return plan && (isSuperAdmin || plan.status === 'PUBLISHED') ? ok(plan) : planNotFound();
    }
    if (!isSuperAdmin) {
      return forbidden();
    }
    switch (route) {
      case 'POST ':
        return this.createPlan(request.body as PlanFields);
      case 'PUT :id':
        return this.updatePlan(id, request.body as UpdatePlanRequest);
      case 'POST :id/publication':
        return this.publishPlan(id);
      default:
        return null;
    }
  }

  private listPlans(query: URLSearchParams, isSuperAdmin: boolean): MockResponse {
    const status = query.get('status');
    if (!isSuperAdmin && status !== 'PUBLISHED') {
      return forbidden();
    }
    const plans = this.plans
      .filter((plan) => status === null || plan.status === status)
      .sort((a, b) => a.name.localeCompare(b.name));
    return ok(pageOf(plans, query));
  }

  private createPlan(body: PlanFields): MockResponse {
    const invalid = this.checkPlan(body, null);
    if (invalid !== null) {
      return invalid;
    }
    const plan: Plan = {
      ...withDefaults(body),
      id: `mock-plan-${this.nextId++}`,
      status: 'DRAFT',
      version: 0,
    };
    this.plans.push(plan);
    return created(plan);
  }

  private updatePlan(id: string | undefined, body: UpdatePlanRequest): MockResponse {
    const current = this.plan(id);
    if (current === undefined) {
      return planNotFound();
    }
    if (body.version !== current.version) {
      return problem(409, 'STALE_STATE');
    }
    const { version, ...fields } = body;
    const invalid = this.checkPlan(fields, current);
    if (invalid !== null) {
      return invalid;
    }
    if (current.status === 'PUBLISHED') {
      const missing = this.missingToPublish(withDefaults(fields));
      if (missing.length > 0) {
        return problem(422, 'PLAN_INCOMPLETE', missing);
      }
    }
    return ok(this.replacePlan({ ...current, ...withDefaults(fields), version: version + 1 }));
  }

  private publishPlan(id: string | undefined): MockResponse {
    const current = this.plan(id);
    if (current === undefined) {
      return planNotFound();
    }
    if (current.status === 'PUBLISHED') {
      return ok(current);
    }
    const missing = this.missingToPublish(current);
    if (missing.length > 0) {
      return problem(422, 'PLAN_INCOMPLETE', missing);
    }
    return ok(this.replacePlan({ ...current, status: 'PUBLISHED', version: current.version + 1 }));
  }

  private checkPlan(fields: PlanFields, current: Plan | null): MockResponse | null {
    const name = fields.name?.trim() ?? '';
    if (name === '') {
      return problem(422, 'VALIDATION_FAILED', [{ field: 'name', code: 'REQUIRED' }]);
    }
    const nameTaken = this.plans.some(
      (plan) => plan.id !== current?.id && plan.name.toLowerCase() === name.toLowerCase(),
    );
    if (nameTaken) {
      return problem(409, 'PLAN_NAME_TAKEN');
    }
    const fieldErrors = this.customFieldErrors(fields.customFields ?? {});
    return fieldErrors.length > 0 ? problem(422, 'VALIDATION_FAILED', fieldErrors) : null;
  }

  private customFieldErrors(values: CustomFieldValues): MockFieldError[] {
    return Object.entries(values).flatMap(([key, value]) => {
      const definition = this.activeCustomField(key);
      const field = `${CUSTOM_FIELDS_PREFIX}${key}`;
      if (definition === undefined) {
        return [{ field, code: 'UNKNOWN_FIELD' }];
      }
      return MOCK_FIELD_TYPE_CHECKS[definition.type](value) ? [] : [{ field, code: 'WRONG_TYPE' }];
    });
  }

  private missingToPublish(fields: PlanFields): MockFieldError[] {
    const basic = BASIC_REQUIRED_TO_PUBLISH.filter((key) => fields[key] === null).map((field) => ({
      field,
      code: 'REQUIRED',
    }));
    const custom = this.fields
      .filter((field) => field.origin === 'CUSTOM' && field.retiredAt === null && field.required)
      .filter((field) => fields.customFields[field.key] === undefined)
      .map((field) => ({ field: `${CUSTOM_FIELDS_PREFIX}${field.key}`, code: 'REQUIRED' }));
    return [...basic, ...custom];
  }

  private handleFields(
    request: MockRequest,
    id: string | undefined,
    subresource: string | undefined,
  ): MockResponse | null {
    const route = routeOf(request.method, id, subresource);
    switch (route) {
      case 'GET ':
        return ok(this.sortedFields());
      case 'POST ':
        return this.createField(request.body as CreatePlanFieldRequest);
      case 'PUT :id':
        return this.updateField(id, request.body as UpdatePlanFieldRequest);
      case 'POST :id/retirement':
        return this.retireField(id);
      default:
        return null;
    }
  }

  private createField(body: CreatePlanFieldRequest): MockResponse {
    const label = body.label.trim();
    if (this.labelTaken(label, null)) {
      return problem(409, 'FIELD_NAME_TAKEN');
    }
    const field: PlanField = {
      id: `mock-field-${this.nextId++}`,
      key: keyOf(label),
      label,
      type: body.type,
      required: body.required,
      origin: 'CUSTOM',
      retiredAt: null,
      version: 0,
    };
    this.fields.push(field);
    return created(field);
  }

  private updateField(id: string | undefined, body: UpdatePlanFieldRequest): MockResponse {
    const current = this.fields.find((field) => field.id === id);
    if (current === undefined) {
      return problem(404, 'PLAN_FIELD_NOT_FOUND');
    }
    if (current.origin === 'BASIC') {
      return problem(422, 'BASIC_FIELD_READ_ONLY');
    }
    if (body.version !== current.version) {
      return problem(409, 'STALE_STATE');
    }
    const label = body.label.trim();
    if (this.labelTaken(label, current.id)) {
      return problem(409, 'FIELD_NAME_TAKEN');
    }
    return ok(
      this.replaceField({
        ...current,
        label,
        required: body.required,
        version: current.version + 1,
      }),
    );
  }

  private retireField(id: string | undefined): MockResponse {
    const current = this.fields.find((field) => field.id === id);
    if (current === undefined) {
      return problem(404, 'PLAN_FIELD_NOT_FOUND');
    }
    if (current.origin === 'BASIC') {
      return problem(422, 'BASIC_FIELD_READ_ONLY');
    }
    if (current.retiredAt !== null) {
      return ok(current);
    }
    return ok(
      this.replaceField({
        ...current,
        retiredAt: new Date().toISOString(),
        version: current.version + 1,
      }),
    );
  }

  private plan(id: string | undefined): Plan | undefined {
    return this.plans.find((plan) => plan.id === id);
  }

  private replacePlan(plan: Plan): Plan {
    this.plans.splice(
      this.plans.findIndex((candidate) => candidate.id === plan.id),
      1,
      plan,
    );
    return plan;
  }

  private replaceField(field: PlanField): PlanField {
    this.fields.splice(
      this.fields.findIndex((candidate) => candidate.id === field.id),
      1,
      field,
    );
    return field;
  }

  private activeCustomField(key: string): PlanField | undefined {
    return this.fields.find(
      (field) => field.origin === 'CUSTOM' && field.retiredAt === null && field.key === key,
    );
  }

  private labelTaken(label: string, exceptId: string | null): boolean {
    return this.fields.some(
      (field) => field.id !== exceptId && field.label.toLowerCase() === label.toLowerCase(),
    );
  }

  private sortedFields(): PlanField[] {
    const custom = this.fields
      .filter((field) => field.origin === 'CUSTOM')
      .sort((a, b) => a.label.localeCompare(b.label));
    return [...BASIC_FIELDS, ...custom];
  }
}

/** Every field of the contract is optional but `name`: a missing one is stored as null. */
function withDefaults(fields: Partial<PlanFields>): PlanFields {
  return {
    name: fields.name?.trim() ?? '',
    price: fields.price ?? null,
    currency: fields.currency ?? null,
    periodicity: fields.periodicity ?? null,
    courtLimit: fields.courtLimit ?? null,
    userLimit: fields.userLimit ?? null,
    customFields: fields.customFields ?? {},
  };
}

function forbidden(): MockResponse {
  return problem(403, 'FORBIDDEN');
}

function planNotFound(): MockResponse {
  return problem(404, 'PLAN_NOT_FOUND');
}
