/// What the app may show for the complex (tenant) a player enters.
sealed class TenantAccess {
  const TenantAccess();
}

/// `200`: the complex is active and its screens can be used.
final class ActiveTenant extends TenantAccess {
  const ActiveTenant({required this.displayName});

  /// Commercial name of the complex.
  final String displayName;
}

/// `403 TENANT_UNAVAILABLE`: suspended or archived. The contract never says
/// which, so the app cannot tell them apart either.
final class UnavailableTenant extends TenantAccess {
  const UnavailableTenant();
}

/// `404 TENANT_NOT_FOUND`: malformed, unknown, retired or purged subdomain.
final class TenantNotFound extends TenantAccess {
  const TenantNotFound();
}

/// The state of the complex could not be checked (no connection or an answer
/// outside the contract). It says nothing about the complex itself.
class TenantCheckFailure implements Exception {
  const TenantCheckFailure();

  @override
  String toString() => 'TenantCheckFailure';
}

/// `GET /tenant` of `contracts/tenancy.openapi.yaml` (`getTenantState`). The
/// complex is the one resolved by the subdomain the implementation talks to.
abstract interface class TenantApi {
  /// The access to the complex, or throws [TenantCheckFailure].
  Future<TenantAccess> getTenantAccess();
}
