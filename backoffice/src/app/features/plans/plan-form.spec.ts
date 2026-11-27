import {
  PLAN_OPTIONS,
  PUBLISHED_PLAN,
  SUPPORT_HOURS_FIELD,
  TOURNAMENTS_FIELD,
} from '../../../testing/platform-fixtures';
import { PlanField } from './plan-fields-api';
import {
  apiFieldErrors,
  planFormFields,
  planFormValues,
  toPlanFields,
  validatePlanForm,
} from './plan-form';

const COURTS_FIELD: PlanField = {
  ...SUPPORT_HOURS_FIELD,
  id: 'courts-field',
  key: 'extra-courts',
  label: 'Canchas extra',
  type: 'NUMBER',
  required: false,
};

describe('plan form', () => {
  const { basic, custom } = planFormFields(PLAN_OPTIONS, [
    SUPPORT_HOURS_FIELD,
    TOURNAMENTS_FIELD,
    COURTS_FIELD,
  ]);
  const fields = [...basic, ...custom];
  const empty = planFormValues(fields, null);
  const complete = {
    ...empty,
    name: 'Premium',
    price: '120,50',
    currency: 'USD',
    periodicity: 'MONTHLY',
    courtLimit: '10',
    userLimit: '0',
    'customFields.support-hours': 'Lunes a viernes',
  };

  it('generates «Otros campos» from the type of each custom field (CA4)', () => {
    expect(basic.map((field) => field.label)).toEqual([
      'Nombre',
      'Precio',
      'Moneda',
      'Periodicidad',
      'Límite de canchas',
      'Límite de usuarios',
    ]);
    expect(custom.map((field) => [field.path, field.kind, field.requiredToPublish])).toEqual([
      ['customFields.support-hours', 'text', true],
      ['customFields.tournaments-included', 'yes-no', false],
      ['customFields.extra-courts', 'decimal', false],
    ]);
    expect(basic[3].options).toEqual([
      { value: 'MONTHLY', label: 'Mensual' },
      { value: 'YEARLY', label: 'Anual' },
    ]);
  });

  it('starts a new plan empty, without preset price, currency or limits', () => {
    expect(Object.values(empty).every((value) => value === '')).toBe(true);
  });

  it('reads the values of an existing plan, booleans and numbers included', () => {
    const values = planFormValues(fields, {
      ...PUBLISHED_PLAN,
      customFields: { 'tournaments-included': false, 'extra-courts': 2 },
    });

    expect(values['price']).toBe('120.00');
    expect(values['courtLimit']).toBe('10');
    expect(values['customFields.tournaments-included']).toBe('false');
    expect(values['customFields.extra-courts']).toBe('2');
    expect(values['customFields.support-hours']).toBe('');
  });

  it('asks only for the name in a draft', () => {
    expect(validatePlanForm(fields, empty, 'draft')).toEqual({
      name: 'Escribe el nombre del plan.',
    });
    expect(validatePlanForm(fields, { ...empty, name: 'Inicial' }, 'draft')).toEqual({});
  });

  it('asks for every basic and required field to publish', () => {
    expect(Object.keys(validatePlanForm(fields, { ...empty, name: 'X' }, 'complete'))).toEqual([
      'price',
      'currency',
      'periodicity',
      'courtLimit',
      'userLimit',
      'customFields.support-hours',
    ]);
    expect(validatePlanForm(fields, complete, 'complete')).toEqual({});
  });

  it('checks the format of what is typed, also in a draft', () => {
    const errors = validatePlanForm(
      fields,
      {
        ...empty,
        name: 'x'.repeat(121),
        price: '12.34567',
        courtLimit: '-1',
        'customFields.extra-courts': 'dos',
      },
      'draft',
    );

    expect(errors).toEqual({
      name: 'Usa como máximo 120 caracteres.',
      price: 'Escribe un precio válido, por ejemplo 120.00 (hasta 4 decimales).',
      courtLimit: 'Escribe un número entero igual o mayor que 0.',
      'customFields.extra-courts': 'Escribe un número, por ejemplo 10 o 2.5.',
    });
  });

  it('builds the api body with nulls for empty values and typed custom values', () => {
    expect(
      toPlanFields(fields, {
        ...complete,
        'customFields.tournaments-included': 'true',
        'customFields.extra-courts': '2,5',
      }),
    ).toEqual({
      name: 'Premium',
      price: '120.50',
      currency: 'USD',
      periodicity: 'MONTHLY',
      courtLimit: 10,
      userLimit: 0,
      customFields: {
        'support-hours': 'Lunes a viernes',
        'tournaments-included': true,
        'extra-courts': 2.5,
      },
    });
    expect(toPlanFields(fields, { ...empty, name: ' Inicial ' })).toEqual({
      name: 'Inicial',
      price: null,
      currency: null,
      periodicity: null,
      courtLimit: null,
      userLimit: null,
      customFields: {},
    });
  });

  it('translates the field errors of the api and ignores fields not on screen', () => {
    expect(
      apiFieldErrors(fields, [
        { field: 'price', code: 'REQUIRED' },
        { field: 'currency', code: 'NOT_ALLOWED' },
        { field: 'customFields.extra-courts', code: 'WRONG_TYPE' },
        { field: 'customFields.retired', code: 'UNKNOWN_FIELD' },
      ]),
    ).toEqual({
      price: 'Completa «Precio».',
      currency: 'Ese valor no está permitido. Elige otro.',
      'customFields.extra-courts': 'Escribe un número, por ejemplo 10 o 2.5.',
    });
  });
});
