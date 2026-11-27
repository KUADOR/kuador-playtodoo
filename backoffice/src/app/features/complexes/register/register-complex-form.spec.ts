import {
  fieldErrorsOfProblem,
  REGISTER_TEXTS,
  validateRegisterForm,
} from './register-complex-form';
import { hasValidSubdomainFormat, SUBDOMAIN_CHECK_TEXTS } from './subdomain-check';

const VALID = {
  displayName: 'Canchas El Sol',
  subdomain: 'canchas-el-sol',
  ownerEmail: 'owner@example.com',
  planId: 'plan-1',
};

describe('hasValidSubdomainFormat (ADR-012)', () => {
  it.each(['abc', 'canchas-el-sol', 'a1-b2', 'x'.repeat(63)])('accepts %s', (candidate) => {
    expect(hasValidSubdomainFormat(candidate)).toBe(true);
  });

  it.each(['ab', 'x'.repeat(64), '-sol', 'sol-', 'el--sol', 'xn--abc', 'El-Sol', 'el_sol'])(
    'rejects %s',
    (candidate) => {
      expect(hasValidSubdomainFormat(candidate)).toBe(false);
    },
  );
});

describe('validateRegisterForm (P3)', () => {
  it('accepts a complete form whose subdomain is available or not checked yet', () => {
    expect(validateRegisterForm(VALID, 'available')).toEqual({});
    expect(validateRegisterForm(VALID, 'checking')).toEqual({});
  });

  it('asks for every missing field', () => {
    expect(
      validateRegisterForm({ displayName: ' ', subdomain: '', ownerEmail: '', planId: '' }, 'idle'),
    ).toEqual({
      displayName: REGISTER_TEXTS.displayNameRequired,
      subdomain: REGISTER_TEXTS.subdomainRequired,
      ownerEmail: REGISTER_TEXTS.ownerEmailRequired,
      planId: REGISTER_TEXTS.planRequired,
    });
  });

  it('rejects a long name, a malformed e-mail and a taken subdomain (CA2)', () => {
    expect(
      validateRegisterForm(
        { ...VALID, displayName: 'x'.repeat(121), ownerEmail: 'owner@' },
        'taken',
      ),
    ).toEqual({
      displayName: REGISTER_TEXTS.displayNameTooLong,
      subdomain: SUBDOMAIN_CHECK_TEXTS.taken,
      ownerEmail: REGISTER_TEXTS.ownerEmailInvalid,
    });
  });
});

describe('fieldErrorsOfProblem', () => {
  it('puts the 422 field errors of the api on the form fields', () => {
    expect(
      fieldErrorsOfProblem([
        { field: 'displayName', code: 'TOO_LONG' },
        { field: 'ownerEmail', code: 'INVALID' },
        { field: 'unknown', code: 'X' },
      ]),
    ).toEqual({
      displayName: REGISTER_TEXTS.displayNameTooLong,
      ownerEmail: REGISTER_TEXTS.ownerEmailInvalid,
    });
  });
});
