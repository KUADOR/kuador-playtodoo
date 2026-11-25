import '../domain/tenant_api.dart';

/// Mock of `GET /tenant` (`contracts/tenancy.openapi.yaml`), used until the
/// app talks to the real `api`. Tests and the debug preview change [access]
/// or [isOffline] to move the complex between its states.
class InMemoryTenantApi implements TenantApi {
  InMemoryTenantApi({required this.access, this.latency = Duration.zero});

  /// What the next calls answer.
  TenantAccess access;

  /// Delay of every answer, to see the loading state while trying the app.
  final Duration latency;

  /// Makes the next calls fail as if the device were offline.
  bool isOffline = false;

  @override
  Future<TenantAccess> getTenantAccess() async {
    if (latency > Duration.zero) {
      await Future<void>.delayed(latency);
    }
    if (isOffline) {
      throw const TenantCheckFailure();
    }
    return access;
  }
}
