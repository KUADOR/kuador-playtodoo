import { SubdomainUnavailableReason } from '../subdomains-api';

/** Live state of the subdomain field of P3. */
export type SubdomainCheck =
  'idle' | 'checking' | 'available' | 'invalid' | 'reserved' | 'taken' | 'unverified';

const MIN_LENGTH = 3;
const MAX_LENGTH = 63;
/** Lower-case letters and digits in groups joined by single hyphens (ADR-012). */
const LABEL_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const PUNYCODE_PREFIX = 'xn--';

/** Format rules of ADR-012, checked before asking the api. */
export function hasValidSubdomainFormat(candidate: string): boolean {
  return (
    candidate.length >= MIN_LENGTH &&
    candidate.length <= MAX_LENGTH &&
    LABEL_PATTERN.test(candidate) &&
    !candidate.startsWith(PUNYCODE_PREFIX)
  );
}

export const UNAVAILABLE_CHECKS: Readonly<Record<SubdomainUnavailableReason, SubdomainCheck>> = {
  INVALID: 'invalid',
  RESERVED: 'reserved',
  TAKEN: 'taken',
};

/** Texts of P3 for each state. `idle` shows nothing. */
export const SUBDOMAIN_CHECK_TEXTS: Readonly<Record<Exclude<SubdomainCheck, 'idle'>, string>> = {
  checking: 'Comprobando…',
  available: 'Disponible',
  invalid:
    'Usa de 3 a 63 letras minúsculas, números o guiones; sin guiones al inicio, al final ni seguidos. No puede empezar con “xn--”.',
  reserved: 'Ese nombre está reservado. Elige otro.',
  taken: 'Ese subdominio ya está en uso. Elige otro.',
  unverified: 'No pudimos comprobar el subdominio. Se comprobará al dar de alta.',
};

/** States that block the registration and show as a field error. */
export function isSubdomainRejected(
  check: SubdomainCheck,
): check is 'invalid' | 'reserved' | 'taken' {
  return check === 'invalid' || check === 'reserved' || check === 'taken';
}

/** Field error of a rejected subdomain; undefined while it may still be used. */
export function subdomainRejectionText(check: SubdomainCheck): string | undefined {
  return isSubdomainRejected(check) ? SUBDOMAIN_CHECK_TEXTS[check] : undefined;
}
