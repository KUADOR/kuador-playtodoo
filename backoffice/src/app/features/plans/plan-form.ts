import { FieldError } from '../../core/api/api-error';
import { FieldOption } from '../../shared/ui/field/field';
import { PlanField } from './plan-fields-api';
import { BASIC_FIELD_LABELS, periodicityLabel } from './plan-texts';
import { Plan, PlanFields, PlanOptions } from './plans-api';

/** How a value is typed in P11; custom fields get theirs from their `type`. */
export type PlanInputKind = 'text' | 'decimal' | 'integer' | 'choice' | 'yes-no';

/** One control of P11 «Crear o editar plan». */
export interface PlanFormField {
  /** Property path of the api, as in `fieldErrors`: `price` or `customFields.support-hours`. */
  readonly path: string;
  readonly label: string;
  readonly kind: PlanInputKind;
  /** A draft needs only the name; publishing (or a published plan) needs these too. */
  readonly requiredToPublish: boolean;
  readonly options: readonly FieldOption[];
  readonly custom: boolean;
}

/** What the person typed, by `path`. Empty string means no value. */
export type PlanFormValues = Readonly<Record<string, string>>;

/** Error text by `path`. */
export type PlanFormErrors = Readonly<Record<string, string>>;

/** `draft` checks only what is typed; `complete` also asks for every field required to publish. */
export type PlanValidationMode = 'draft' | 'complete';

const CUSTOM_FIELDS_PREFIX = 'customFields.';
const NAME_MAX_LENGTH = 120;
const MONEY_PATTERN = /^\d{1,15}(\.\d{1,4})?$/;
const INTEGER_PATTERN = /^\d+$/;
const NUMBER_PATTERN = /^-?\d+(\.\d+)?$/;
const DECIMAL_COMMA = ',';
const DECIMAL_POINT = '.';
const YES = 'true';
const NO = 'false';

export const YES_NO_OPTIONS: readonly FieldOption[] = [
  { value: YES, label: 'Sí' },
  { value: NO, label: 'No' },
];

const MESSAGES = {
  nameRequired: 'Escribe el nombre del plan.',
  nameTooLong: `Usa como máximo ${NAME_MAX_LENGTH} caracteres.`,
  priceInvalid: 'Escribe un precio válido, por ejemplo 120.00 (hasta 4 decimales).',
  integerInvalid: 'Escribe un número entero igual o mayor que 0.',
  numberInvalid: 'Escribe un número, por ejemplo 10 o 2.5.',
  notAllowed: 'Ese valor no está permitido. Elige otro.',
  unknownField: 'Este campo ya no se usa en los planes. Recarga la página.',
  invalid: 'Revisa este valor.',
} as const;

function basicField(
  path: keyof PlanFields,
  kind: PlanInputKind,
  options: readonly FieldOption[] = [],
): PlanFormField {
  return {
    path,
    label: BASIC_FIELD_LABELS[path],
    kind,
    requiredToPublish: true,
    options,
    custom: false,
  };
}

const CUSTOM_KIND: Readonly<Record<PlanField['type'], PlanInputKind>> = {
  TEXT: 'text',
  NUMBER: 'decimal',
  BOOLEAN: 'yes-no',
};

/**
 * Controls of P11: the basic fields («Datos básicos») and one per custom field that is not
 * retired («Otros campos»), generated from its type.
 */
export function planFormFields(
  options: PlanOptions,
  customFields: readonly PlanField[],
): { readonly basic: PlanFormField[]; readonly custom: PlanFormField[] } {
  return {
    basic: [
      basicField('name', 'text'),
      basicField('price', 'decimal'),
      basicField(
        'currency',
        'choice',
        options.currencies.map((currency) => ({ value: currency, label: currency })),
      ),
      basicField(
        'periodicity',
        'choice',
        options.periodicities.map((value) => ({ value, label: periodicityLabel(value) })),
      ),
      basicField('courtLimit', 'integer'),
      basicField('userLimit', 'integer'),
    ],
    custom: customFields.map((field) => ({
      path: `${CUSTOM_FIELDS_PREFIX}${field.key}`,
      label: field.label,
      kind: CUSTOM_KIND[field.type],
      requiredToPublish: field.required,
      options: field.type === 'BOOLEAN' ? YES_NO_OPTIONS : [],
      custom: true,
    })),
  };
}

function customKey(path: string): string {
  return path.slice(CUSTOM_FIELDS_PREFIX.length);
}

function basicValue(value: string | number | null): string {
  return value === null ? '' : String(value);
}

/** Values of an existing plan; a new plan starts empty (no preset price, currency or limits). */
export function planFormValues(
  fields: readonly PlanFormField[],
  plan: Plan | null,
): PlanFormValues {
  return Object.fromEntries(
    fields.map((field) => {
      if (plan === null) {
        return [field.path, ''];
      }
      const value = field.custom
        ? plan.customFields[customKey(field.path)]
        : plan[field.path as keyof PlanFields];
      return [field.path, value === undefined ? '' : basicValue(value as string | number | null)];
    }),
  );
}

/** People in Spanish-speaking countries often type «120,50». */
function normalizeDecimal(value: string): string {
  return value.trim().replace(DECIMAL_COMMA, DECIMAL_POINT);
}

function requiredMessage(field: PlanFormField): string {
  if (field.path === 'name') {
    return MESSAGES.nameRequired;
  }
  return field.kind === 'choice' || field.kind === 'yes-no'
    ? `Elige un valor para «${field.label}».`
    : `Completa «${field.label}».`;
}

function formatError(field: PlanFormField, value: string): string | null {
  switch (field.kind) {
    case 'text':
      return field.path === 'name' && value.length > NAME_MAX_LENGTH ? MESSAGES.nameTooLong : null;
    case 'decimal': {
      const pattern = field.custom ? NUMBER_PATTERN : MONEY_PATTERN;
      if (pattern.test(normalizeDecimal(value))) {
        return null;
      }
      return field.custom ? MESSAGES.numberInvalid : MESSAGES.priceInvalid;
    }
    case 'integer':
      return INTEGER_PATTERN.test(value) ? null : MESSAGES.integerInvalid;
    default:
      return null;
  }
}

/** Errors per field, in the order of `fields` (the order of the error summary). */
export function validatePlanForm(
  fields: readonly PlanFormField[],
  values: PlanFormValues,
  mode: PlanValidationMode,
): PlanFormErrors {
  const errors: Record<string, string> = {};
  for (const field of fields) {
    const value = (values[field.path] ?? '').trim();
    const required = field.path === 'name' || (mode === 'complete' && field.requiredToPublish);
    const error =
      value === '' ? (required ? requiredMessage(field) : null) : formatError(field, value);
    if (error !== null) {
      errors[field.path] = error;
    }
  }
  return errors;
}

function customValue(field: PlanFormField, value: string): string | number | boolean {
  switch (field.kind) {
    case 'decimal':
      return Number(normalizeDecimal(value));
    case 'yes-no':
      return value === YES;
    default:
      return value;
  }
}

function optional(value: string): string | null {
  return value === '' ? null : value;
}

function optionalInteger(value: string): number | null {
  return value === '' ? null : Number(value);
}

/**
 * Body of `POST /plans` and `PUT /plans/{id}` from valid values. Empty values are sent as `null`
 * or left out of `customFields`; only the custom fields on screen (not retired) are sent, since
 * the api rejects retired ones.
 */
export function toPlanFields(fields: readonly PlanFormField[], values: PlanFormValues): PlanFields {
  const value = (path: string) => (values[path] ?? '').trim();
  const customFields: Record<string, string | number | boolean> = {};
  for (const field of fields.filter((candidate) => candidate.custom)) {
    if (value(field.path) !== '') {
      customFields[customKey(field.path)] = customValue(field, value(field.path));
    }
  }
  return {
    name: value('name'),
    price: optional(normalizeDecimal(value('price'))),
    currency: optional(value('currency')),
    periodicity: optional(value('periodicity')),
    courtLimit: optionalInteger(value('courtLimit')),
    userLimit: optionalInteger(value('userLimit')),
    customFields,
  };
}

/** Spanish text of the `fieldErrors` the api answers (`VALIDATION_FAILED`, `PLAN_INCOMPLETE`). */
export function apiFieldErrors(
  fields: readonly PlanFormField[],
  fieldErrors: readonly FieldError[],
): PlanFormErrors {
  const byPath = new Map(fields.map((field) => [field.path, field]));
  const errors: Record<string, string> = {};
  for (const { field: path, code } of fieldErrors) {
    const field = byPath.get(path);
    if (field) {
      errors[path] = apiFieldMessage(field, code);
    }
  }
  return errors;
}

function apiFieldMessage(field: PlanFormField, code: string): string {
  switch (code) {
    case 'REQUIRED':
      return requiredMessage(field);
    case 'TOO_LONG':
      return MESSAGES.nameTooLong;
    case 'NOT_ALLOWED':
      return MESSAGES.notAllowed;
    case 'UNKNOWN_FIELD':
      return MESSAGES.unknownField;
    case 'WRONG_TYPE':
      return formatError(field, '') ?? MESSAGES.invalid;
    default:
      return MESSAGES.invalid;
  }
}
