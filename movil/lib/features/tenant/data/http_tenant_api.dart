import 'dart:async';
import 'dart:convert';
import 'dart:io';

import '../domain/tenant_api.dart';

/// `GET /tenant` (`contracts/tenancy.openapi.yaml` · `getTenantState`) over
/// HTTP, on the host of one complex. The endpoint is public: no credentials
/// are sent.
class HttpTenantApi implements TenantApi {
  HttpTenantApi({
    required this.apiBaseUrl,
    HttpClient? httpClient,
    this.timeout = defaultTimeout,
  }) : _httpClient = httpClient ?? HttpClient();

  static const Duration defaultTimeout = Duration(seconds: 10);
  static const String _tenantPath = '/tenant';
  static const String _acceptedTypes =
      'application/json, application/problem+json';

  /// Problem codes that are an answer about the complex, not a failure of
  /// the check.
  static const Map<String, TenantAccess> _accessByProblemCode = {
    'TENANT_UNAVAILABLE': UnavailableTenant(),
    'TENANT_NOT_FOUND': TenantNotFound(),
  };

  /// Api of the complex, for example `https://canchaselsol.playtodoo.com/api/v1`.
  final Uri apiBaseUrl;

  /// Longest wait for an answer before the check counts as failed.
  final Duration timeout;

  final HttpClient _httpClient;

  @override
  Future<TenantAccess> getTenantAccess() async {
    try {
      final (statusCode, body) = await _get().timeout(timeout);
      return _accessOf(statusCode, body);
    } on IOException {
      // No connection, a refused socket or a broken HTTP answer.
      throw const TenantCheckFailure();
    } on TimeoutException {
      throw const TenantCheckFailure();
    }
  }

  Future<(int, String)> _get() async {
    final request = await _httpClient.getUrl(
      apiBaseUrl.replace(path: '${apiBaseUrl.path}$_tenantPath'),
    );
    request.headers.set(HttpHeaders.acceptHeader, _acceptedTypes);
    final response = await request.close();
    final body = await response.transform(utf8.decoder).join();
    return (response.statusCode, body);
  }

  /// Any answer outside the contract (for example a proxy error page) is a
  /// failed check: the state of the complex is never guessed.
  static TenantAccess _accessOf(int statusCode, String body) {
    final json = _jsonObject(body);
    if (statusCode == HttpStatus.ok) {
      final displayName = json?['displayName'];
      if (displayName is String) return ActiveTenant(displayName: displayName);
      throw const TenantCheckFailure();
    }
    final access = _accessByProblemCode[json?['code']];
    if (access == null) throw const TenantCheckFailure();
    return access;
  }

  static Map<String, Object?>? _jsonObject(String body) {
    try {
      final decoded = jsonDecode(body);
      return decoded is Map<String, Object?> ? decoded : null;
    } on FormatException {
      // Not JSON: the caller reports it as an answer outside the contract.
      return null;
    }
  }
}
