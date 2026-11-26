import { SAME_ORIGIN_API_BASE_URL, resolveApiBaseUrl } from './api-base-url';

describe('resolveApiBaseUrl', () => {
  it('points the backoffice host to the global api host of the same root domain', () => {
    expect(
      resolveApiBaseUrl({ protocol: 'https:', hostname: 'admin.playtodoo.com', port: '' }),
    ).toBe('https://api.playtodoo.com/api/v1');
  });

  it('keeps the port of the local proxy', () => {
    expect(
      resolveApiBaseUrl({ protocol: 'http:', hostname: 'admin.localhost', port: '8000' }),
    ).toBe('http://api.localhost:8000/api/v1');
  });

  it.each(['localhost', '127.0.0.1', 'admin', 'backoffice.playtodoo.com'])(
    'uses the same origin on %s',
    (hostname) => {
      expect(resolveApiBaseUrl({ protocol: 'http:', hostname, port: '4200' })).toBe(
        SAME_ORIGIN_API_BASE_URL,
      );
    },
  );
});
