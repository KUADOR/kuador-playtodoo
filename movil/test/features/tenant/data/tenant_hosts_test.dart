import 'package:flutter_test/flutter_test.dart';
import 'package:playtodoo/features/tenant/data/tenant_hosts.dart';

void main() {
  test('builds the api of a complex on its own subdomain in production', () {
    expect(
      TenantHosts.apiBaseUrl(
        'canchaselsol',
        urlPattern: TenantHosts.productionUrlPattern,
      ),
      Uri.parse('https://canchaselsol.playtodoo.com/api/v1'),
    );
  });

  test('builds the api of a complex on the local proxy', () {
    expect(
      TenantHosts.apiBaseUrl(
        'canchaselsol',
        urlPattern: 'http://{subdomain}.localhost:8000',
      ),
      Uri.parse('http://canchaselsol.localhost:8000/api/v1'),
    );
  });

  test('defaults to production when no pattern is configured', () {
    expect(TenantHosts.configuredUrlPattern, TenantHosts.productionUrlPattern);
  });

  test('refuses a value that is not a host label', () {
    for (final value in ['', 'Canchas', 'el sol', 'a.b', 'el/sol', 'x' * 64]) {
      expect(TenantHosts.isHostLabel(value), isFalse, reason: value);
      expect(() => TenantHosts.apiBaseUrl(value), throwsArgumentError);
    }
  });
}
