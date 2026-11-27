import { FieldError } from '../../../core/api/api-error';
import { SubdomainCheck, subdomainRejectionText } from './subdomain-check';

export const DISPLAY_NAME_MAX_LENGTH = 120;
export const OWNER_EMAIL_MAX_LENGTH = 254;
/** Enough to catch typos; the api validates the e-mail format again. */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export interface RegisterComplexValues {
  readonly displayName: string;
  readonly subdomain: string;
  readonly ownerEmail: string;
  readonly planId: string;
}

export type RegisterField = keyof RegisterComplexValues;

export type RegisterFieldErrors = Partial<Record<RegisterField, string>>;

/** Order of the fields on screen: the focus goes to the first one with an error. */
export const REGISTER_FIELD_ORDER: readonly RegisterField[] = [
  'displayName',
  'subdomain',
  'ownerEmail',
  'planId',
];

export const REGISTER_TEXTS = {
  displayNameRequired: 'Escribe el nombre del complejo.',
  displayNameTooLong: `Usa como máximo ${DISPLAY_NAME_MAX_LENGTH} caracteres.`,
  subdomainRequired: 'Escribe el subdominio.',
  ownerEmailRequired: 'Escribe el correo del propietario.',
  ownerEmailInvalid: 'Escribe un correo válido, por ejemplo nombre@dominio.com.',
  planRequired: 'Elige un plan.',
  planNotPublished: 'Ese plan ya no está publicado. Elige otro.',
  invalidField: 'Revisa este dato.',
} as const;

function displayNameError(value: string): string | undefined {
  if (!value.trim()) {
    return REGISTER_TEXTS.displayNameRequired;
  }
  return value.trim().length > DISPLAY_NAME_MAX_LENGTH
    ? REGISTER_TEXTS.displayNameTooLong
    : undefined;
}

function subdomainError(value: string, check: SubdomainCheck): string | undefined {
  if (!value) {
    return REGISTER_TEXTS.subdomainRequired;
  }
  return subdomainRejectionText(check);
}

function ownerEmailError(value: string): string | undefined {
  if (!value.trim()) {
    return REGISTER_TEXTS.ownerEmailRequired;
  }
  return EMAIL_PATTERN.test(value.trim()) ? undefined : REGISTER_TEXTS.ownerEmailInvalid;
}

/** Errors of P3 before sending; an empty object means the form can be sent. */
export function validateRegisterForm(
  values: RegisterComplexValues,
  subdomainCheck: SubdomainCheck,
): RegisterFieldErrors {
  const errors: RegisterFieldErrors = {
    displayName: displayNameError(values.displayName),
    subdomain: subdomainError(values.subdomain, subdomainCheck),
    ownerEmail: ownerEmailError(values.ownerEmail),
    planId: values.planId ? undefined : REGISTER_TEXTS.planRequired,
  };
  return Object.fromEntries(
    Object.entries(errors).filter(([, message]) => message !== undefined),
  ) as RegisterFieldErrors;
}

/** `fieldErrors` of a `422 VALIDATION_FAILED`, on the fields of the form. */
export function fieldErrorsOfProblem(fieldErrors: readonly FieldError[]): RegisterFieldErrors {
  const errors: RegisterFieldErrors = {};
  for (const error of fieldErrors) {
    const field = REGISTER_FIELD_ORDER.find((candidate) => candidate === error.field);
    if (field === 'displayName' && error.code === 'TOO_LONG') {
      errors.displayName = REGISTER_TEXTS.displayNameTooLong;
    } else if (field === 'ownerEmail') {
      errors.ownerEmail = REGISTER_TEXTS.ownerEmailInvalid;
    } else if (field) {
      errors[field] = REGISTER_TEXTS.invalidField;
    }
  }
  return errors;
}
