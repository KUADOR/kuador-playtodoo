/// Hosts of the complexes. A complex is always reached on its own subdomain
/// (ADR-002): the `api` resolves the complex from the host of the request.
abstract final class TenantHosts {
  /// Production web of a complex; the same setting as the api's
  /// `playtodoo.tenant-url-pattern`.
  static const String productionUrlPattern =
      'https://{subdomain}.playtodoo.com';

  /// Set per environment with
  /// `--dart-define=PLAYTODOO_TENANT_URL_PATTERN=http://{subdomain}.localhost:8000`.
  static const String configuredUrlPattern = String.fromEnvironment(
    'PLAYTODOO_TENANT_URL_PATTERN',
    defaultValue: productionUrlPattern,
  );

  static const String _subdomainPlaceholder = '{subdomain}';
  static const String _apiPath = '/api/v1';

  /// One DNS label: the characters a subdomain may have (ADR-012). Whether it
  /// is well formed and belongs to a complex is decided by the api.
  static final RegExp _label = RegExp(r'^[a-z0-9-]{1,63}$');

  /// Whether [subdomain] can be put in a host name.
  static bool isHostLabel(String subdomain) => _label.hasMatch(subdomain);

  /// Base URL of the api of the complex on [subdomain], for example
  /// `https://canchaselsol.playtodoo.com/api/v1`.
  static Uri apiBaseUrl(
    String subdomain, {
    String urlPattern = configuredUrlPattern,
  }) {
    if (!isHostLabel(subdomain)) {
      throw ArgumentError.value(subdomain, 'subdomain', 'Not a host label');
    }
    return Uri.parse(
      '${urlPattern.replaceAll(_subdomainPlaceholder, subdomain)}$_apiPath',
    );
  }
}
