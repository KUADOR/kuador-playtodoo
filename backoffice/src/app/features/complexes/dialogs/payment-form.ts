import { FieldError } from '../../../core/api/api-error';
import { todayAsCalendarDate } from '../../../core/format/date-format';
import { PaymentMethod } from '../complexes-api';

/** Up to 15 integer digits and 4 decimals (`Money` of the contract); a comma is accepted too. */
const AMOUNT_PATTERN = /^\d{1,15}([.,]\d{1,4})?$/;
const CALENDAR_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

export const PAYMENT_TEXTS = {
  amountRequired: 'Escribe el importe.',
  amountInvalid: 'Escribe un importe válido, por ejemplo 120.50.',
  amountNotPositive: 'El importe debe ser mayor que 0.',
  dateRequired: 'Elige la fecha del pago.',
  dateInFuture: 'La fecha del pago no puede ser futura.',
  methodRequired: 'Elige el medio de pago.',
} as const;

export interface PaymentFormValues {
  readonly amount: string;
  readonly paidOn: string;
  readonly method: PaymentMethod | '';
}

export type PaymentField = keyof PaymentFormValues;
export type PaymentFieldErrors = Partial<Record<PaymentField, string>>;
export const PAYMENT_FIELD_ORDER: readonly PaymentField[] = ['amount', 'paidOn', 'method'];

/** The amount as the contract expects it: a decimal string with a dot. */
export function normalizeAmount(amount: string): string {
  return amount.trim().replace(',', '.');
}

function amountError(amount: string): string | undefined {
  const value = amount.trim();
  if (!value) {
    return PAYMENT_TEXTS.amountRequired;
  }
  if (!AMOUNT_PATTERN.test(value)) {
    return PAYMENT_TEXTS.amountInvalid;
  }
  return /[1-9]/.test(value) ? undefined : PAYMENT_TEXTS.amountNotPositive;
}

function dateError(paidOn: string, today: string): string | undefined {
  if (!CALENDAR_DATE_PATTERN.test(paidOn)) {
    return PAYMENT_TEXTS.dateRequired;
  }
  // ISO dates compare correctly as text.
  return paidOn > today ? PAYMENT_TEXTS.dateInFuture : undefined;
}

export function validatePaymentForm(
  values: PaymentFormValues,
  today: string = todayAsCalendarDate(),
): PaymentFieldErrors {
  const errors: PaymentFieldErrors = {
    amount: amountError(values.amount),
    paidOn: dateError(values.paidOn, today),
    method: values.method ? undefined : PAYMENT_TEXTS.methodRequired,
  };
  return Object.fromEntries(
    Object.entries(errors).filter(([, message]) => message !== undefined),
  ) as PaymentFieldErrors;
}

const PROBLEM_FIELD_TEXTS: Readonly<Record<string, string>> = {
  'amount:NOT_POSITIVE': PAYMENT_TEXTS.amountNotPositive,
  'paidOn:DATE_IN_FUTURE': PAYMENT_TEXTS.dateInFuture,
};

const FIELD_FALLBACK_TEXTS: Readonly<Record<PaymentField, string>> = {
  amount: PAYMENT_TEXTS.amountInvalid,
  paidOn: PAYMENT_TEXTS.dateRequired,
  method: PAYMENT_TEXTS.methodRequired,
};

/** `fieldErrors` of a `422 VALIDATION_FAILED` on the fields of the dialog. */
export function paymentErrorsOfProblem(fieldErrors: readonly FieldError[]): PaymentFieldErrors {
  const errors: PaymentFieldErrors = {};
  for (const error of fieldErrors) {
    const field = PAYMENT_FIELD_ORDER.find((candidate) => candidate === error.field);
    if (field) {
      errors[field] =
        PROBLEM_FIELD_TEXTS[`${error.field}:${error.code}`] ?? FIELD_FALLBACK_TEXTS[field];
    }
  }
  return errors;
}
