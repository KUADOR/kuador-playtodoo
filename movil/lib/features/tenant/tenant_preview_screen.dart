import 'package:material_ui/material_ui.dart';

import '../../core/design/tokens.g.dart';
import '../../shared/ui/app_button.dart';
import 'data/in_memory_tenant_api.dart';
import 'domain/tenant_api.dart';
import 'presentation/tenant_gate.dart';

/// Debug-only entry to try the complex states against the contract mock,
/// until the app talks to the real `api`. The complex name is sample data.
class TenantPreviewScreen extends StatelessWidget {
  const TenantPreviewScreen({super.key});

  static const ActiveTenant sampleTenant = ActiveTenant(
    displayName: 'Complejo de prueba',
  );
  static const Duration _sampleLatency = Duration(milliseconds: 600);

  @override
  Widget build(BuildContext context) {
    final options = <(String, InMemoryTenantApi Function())>[
      ('Complejo activo', () => _api(sampleTenant)),
      ('Complejo no disponible', () => _api(const UnavailableTenant())),
      ('Complejo no encontrado', () => _api(const TenantNotFound())),
      ('Sin conexión', () => _api(sampleTenant)..isOffline = true),
    ];
    return Scaffold(
      appBar: AppBar(title: const Text('Estados del complejo')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppTokens.space4),
          children: [
            for (final (index, (label, createApi)) in options.indexed) ...[
              if (index > 0) const SizedBox(height: AppTokens.space3),
              AppButton(
                label: label,
                variant: AppButtonVariant.secondary,
                onPressed: () => _enter(context, createApi()),
              ),
            ],
          ],
        ),
      ),
    );
  }

  static InMemoryTenantApi _api(TenantAccess access) =>
      InMemoryTenantApi(access: access, latency: _sampleLatency);

  void _enter(BuildContext context, InMemoryTenantApi api) {
    final navigator = Navigator.of(context);
    navigator.push(
      MaterialPageRoute<void>(
        builder: (_) => TenantGate(
          api: api,
          onGoToPlatform: () => navigator.popUntil((route) => route.isFirst),
          complexBuilder: (_) => _SampleComplexScreen(api: api),
        ),
      ),
    );
  }
}

/// Stand-in for the screens of the complex. It can suspend the complex while
/// the player is inside, to see the gate replace its navigation.
class _SampleComplexScreen extends StatelessWidget {
  const _SampleComplexScreen({required this.api});

  final InMemoryTenantApi api;

  @override
  Widget build(BuildContext context) {
    final tenant = TenantGate.of(context).tenant;
    return Scaffold(
      appBar: AppBar(title: Text(tenant.displayName)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(AppTokens.space4),
          child: AppButton(
            label: 'Suspender el complejo',
            variant: AppButtonVariant.danger,
            onPressed: () {
              api.access = const UnavailableTenant();
              TenantGate.recheck(context);
            },
          ),
        ),
      ),
    );
  }
}
