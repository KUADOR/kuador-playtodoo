import 'package:material_ui/material_ui.dart';

import '../domain/tenant_api.dart';
import 'tenant_status_views.dart';

/// Entry to a complex: it checks the complex with `GET /tenant` and shows its
/// screens only while it is active.
///
/// The screens of the complex live in a navigator owned by the gate. When the
/// complex turns out to be unavailable or not found, the gate replaces that
/// whole navigator with the state screen, so a player with an open session or
/// booking in the complex cannot keep using it.
///
/// The complex is checked again, without hiding its screens, when the app
/// comes back to the foreground and when a screen of the complex calls
/// [TenantGate.recheck] (for example after the api rejects a request).
class TenantGate extends StatefulWidget {
  const TenantGate({
    super.key,
    required this.api,
    required this.complexBuilder,
    required this.onGoToPlatform,
  });

  final TenantApi api;

  /// First screen of the complex. Read the complex with [TenantGate.of].
  final WidgetBuilder complexBuilder;

  /// «Ir a Playtodoo»: back to the start or search of the app.
  final VoidCallback onGoToPlatform;

  /// The active complex of the gate above [context].
  static TenantGateScope of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<TenantGateScope>();
    assert(scope != null, 'No TenantGate above this context.');
    return scope!;
  }

  /// Checks the complex again; the gate leaves it if it is no longer active.
  static Future<void> recheck(BuildContext context) {
    final scope = context.getInheritedWidgetOfExactType<TenantGateScope>();
    assert(scope != null, 'No TenantGate above this context.');
    return scope!._recheck();
  }

  @override
  State<TenantGate> createState() => _TenantGateState();
}

/// The active complex, for the screens inside a [TenantGate].
class TenantGateScope extends InheritedWidget {
  const TenantGateScope._({
    required this.tenant,
    required this._recheck,
    required super.child,
  });

  final ActiveTenant tenant;
  final Future<void> Function() _recheck;

  @override
  bool updateShouldNotify(TenantGateScope oldWidget) =>
      tenant.displayName != oldWidget.tenant.displayName;
}

class _TenantGateState extends State<TenantGate> {
  final GlobalKey<NavigatorState> _complexNavigatorKey = GlobalKey();
  late final AppLifecycleListener _lifecycleListener;

  /// Null while checking.
  TenantAccess? _access;
  bool _checkFailed = false;

  /// Only the answer to the latest check is applied.
  int _checkGeneration = 0;

  @override
  void initState() {
    super.initState();
    _lifecycleListener = AppLifecycleListener(onResume: _recheck);
    _load();
  }

  @override
  void dispose() {
    _lifecycleListener.dispose();
    super.dispose();
  }

  /// «Reintentar»: checks again from the loading state.
  void _retry() {
    setState(() {
      _checkFailed = false;
      _access = null;
    });
    _load();
  }

  Future<void> _load() async {
    final generation = ++_checkGeneration;
    try {
      final access = await widget.api.getTenantAccess();
      _apply(generation, access: access, checkFailed: false);
    } on TenantCheckFailure {
      _apply(generation, access: null, checkFailed: true);
    }
  }

  /// Checks an active complex while its screens stay visible. A failed check
  /// keeps the player inside: only an answer about the complex takes them out.
  Future<void> _recheck() async {
    if (_access is! ActiveTenant) return;
    final generation = ++_checkGeneration;
    try {
      final access = await widget.api.getTenantAccess();
      _apply(generation, access: access, checkFailed: false);
    } on TenantCheckFailure {
      return;
    }
  }

  void _apply(
    int generation, {
    required TenantAccess? access,
    required bool checkFailed,
  }) {
    if (!mounted || generation != _checkGeneration) return;
    setState(() {
      _access = access;
      _checkFailed = checkFailed;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_checkFailed) return TenantCheckFailedView(onRetry: _retry);
    return switch (_access) {
      null => const TenantCheckingView(),
      final ActiveTenant tenant => _complex(tenant),
      UnavailableTenant() => _blocked(BlockedTenantKind.unavailable),
      TenantNotFound() => _blocked(BlockedTenantKind.notFound),
    };
  }

  Widget _blocked(BlockedTenantKind kind) =>
      TenantBlockedView(kind: kind, onGoToPlatform: widget.onGoToPlatform);

  Widget _complex(ActiveTenant tenant) {
    return TenantGateScope._(
      tenant: tenant,
      recheck: _recheck,
      child: NavigatorPopHandler<Object?>(
        onPopWithResult: (result) =>
            _complexNavigatorKey.currentState?.maybePop(result),
        child: Navigator(
          key: _complexNavigatorKey,
          onGenerateRoute: (settings) => MaterialPageRoute<void>(
            settings: settings,
            builder: widget.complexBuilder,
          ),
        ),
      ),
    );
  }
}
