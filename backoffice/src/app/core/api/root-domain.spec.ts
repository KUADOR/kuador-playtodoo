import { resolveRootDomain, tenantHost } from './root-domain';

describe('resolveRootDomain', () => {
  it('takes the root from the backoffice host', () => {
    expect(resolveRootDomain({ hostname: 'admin.playtodoo.com' })).toBe('playtodoo.com');
    expect(resolveRootDomain({ hostname: 'admin.localhost' })).toBe('localhost');
  });

  it.each(['localhost', 'backoffice.example.com'])(
    'uses %s itself on any other host',
    (hostname) => {
      expect(resolveRootDomain({ hostname })).toBe(hostname);
    },
  );

  it('builds the host of a complex', () => {
    expect(tenantHost('canchas-el-sol', 'playtodoo.com')).toBe('canchas-el-sol.playtodoo.com');
  });
});
