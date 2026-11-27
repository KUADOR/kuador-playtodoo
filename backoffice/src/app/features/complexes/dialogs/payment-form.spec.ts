import {
  normalizeAmount,
  PAYMENT_TEXTS,
  paymentErrorsOfProblem,
  validatePaymentForm,
} from './payment-form';

const TODAY = '2026-12-10';

describe('validatePaymentForm (CA7)', () => {
  it('accepts an amount with a dot or a comma and a date up to today', () => {
    expect(
      validatePaymentForm({ amount: '120.50', paidOn: TODAY, method: 'TRANSFER' }, TODAY),
    ).toEqual({});
    expect(
      validatePaymentForm({ amount: '120,5', paidOn: '2026-12-01', method: 'INVOICE' }, TODAY),
    ).toEqual({});
  });

  it('rejects a missing, malformed or zero amount', () => {
    const errorOf = (amount: string) =>
      validatePaymentForm({ amount, paidOn: TODAY, method: 'TRANSFER' }, TODAY).amount;

    expect(errorOf('')).toBe(PAYMENT_TEXTS.amountRequired);
    expect(errorOf('12.345678')).toBe(PAYMENT_TEXTS.amountInvalid);
    expect(errorOf('-5')).toBe(PAYMENT_TEXTS.amountInvalid);
    expect(errorOf('0.00')).toBe(PAYMENT_TEXTS.amountNotPositive);
  });

  it('rejects a missing or future date and a missing method', () => {
    expect(validatePaymentForm({ amount: '1', paidOn: '', method: '' }, TODAY)).toEqual({
      paidOn: PAYMENT_TEXTS.dateRequired,
      method: PAYMENT_TEXTS.methodRequired,
    });
    expect(
      validatePaymentForm({ amount: '1', paidOn: '2026-12-11', method: 'TRANSFER' }, TODAY).paidOn,
    ).toBe(PAYMENT_TEXTS.dateInFuture);
  });
});

describe('normalizeAmount and paymentErrorsOfProblem', () => {
  it('sends the amount with a dot', () => {
    expect(normalizeAmount(' 120,50 ')).toBe('120.50');
  });

  it('maps the api field errors to the dialog texts', () => {
    expect(
      paymentErrorsOfProblem([
        { field: 'amount', code: 'NOT_POSITIVE' },
        { field: 'paidOn', code: 'DATE_IN_FUTURE' },
        { field: 'method', code: 'NOT_ALLOWED' },
      ]),
    ).toEqual({
      amount: PAYMENT_TEXTS.amountNotPositive,
      paidOn: PAYMENT_TEXTS.dateInFuture,
      method: PAYMENT_TEXTS.methodRequired,
    });
  });
});
