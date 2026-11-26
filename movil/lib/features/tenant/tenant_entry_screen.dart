import 'package:material_ui/material_ui.dart';

import '../../core/design/tokens.g.dart';
import '../../shared/ui/app_button.dart';
import '../../shared/ui/app_field.dart';
import 'data/http_tenant_api.dart';
import 'data/tenant_hosts.dart';
import 'domain/tenant_api.dart';
import 'presentation/tenant_gate.dart';

/// Debug-only entry to a complex of the real `api` by its subdomain, until the
/// search of complexes (ADR-008) gives the player the complex to enter.
class TenantEntryScreen extends StatefulWidget {
  const TenantEntryScreen({super.key, this.apiForSubdomain = httpApiFor});

  /// Api of the complex on a valid subdomain.
  final TenantApi Function(String subdomain) apiForSubdomain;

  static const String title = 'Entrar a un complejo';
  static const String subdomainLabel = 'Subdominio del complejo';
  static const String subdomainHelp =
      'La primera parte de la dirección del complejo.';
  static const String subdomainInvalid =
      'Usa solo letras minúsculas, números y guiones.';
  static const String enter = 'Entrar';

  static TenantApi httpApiFor(String subdomain) =>
      HttpTenantApi(apiBaseUrl: TenantHosts.apiBaseUrl(subdomain));

  @override
  State<TenantEntryScreen> createState() => _TenantEntryScreenState();
}

class _TenantEntryScreenState extends State<TenantEntryScreen> {
  final TextEditingController _subdomain = TextEditingController();
  String? _error;

  @override
  void dispose() {
    _subdomain.dispose();
    super.dispose();
  }

  void _enter() {
    final subdomain = _subdomain.text.trim().toLowerCase();
    if (!TenantHosts.isHostLabel(subdomain)) {
      setState(() => _error = TenantEntryScreen.subdomainInvalid);
      return;
    }
    setState(() => _error = null);
    final navigator = Navigator.of(context);
    navigator.push(
      MaterialPageRoute<void>(
        builder: (_) => TenantGate(
          api: widget.apiForSubdomain(subdomain),
          onGoToPlatform: () => navigator.popUntil((route) => route.isFirst),
          complexBuilder: (_) => const _ComplexStartScreen(),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text(TenantEntryScreen.title)),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppTokens.space4),
          children: [
            AppField<String>.text(
              label: TenantEntryScreen.subdomainLabel,
              helperText: TenantEntryScreen.subdomainHelp,
              errorText: _error,
              controller: _subdomain,
              keyboardType: TextInputType.url,
            ),
            const SizedBox(height: AppTokens.space4),
            AppButton(label: TenantEntryScreen.enter, onPressed: _enter),
          ],
        ),
      ),
    );
  }
}

/// Stand-in for the first screen of the complex, until its screens arrive
/// with their epics: the name the api answered, in the app bar.
class _ComplexStartScreen extends StatelessWidget {
  const _ComplexStartScreen();

  static const String entered = 'Entraste al complejo.';

  @override
  Widget build(BuildContext context) {
    final tenant = TenantGate.of(context).tenant;
    return Scaffold(
      appBar: AppBar(title: Text(tenant.displayName)),
      body: const SafeArea(
        child: Center(
          child: Padding(
            padding: EdgeInsets.all(AppTokens.space4),
            child: Text(entered, textAlign: TextAlign.center),
          ),
        ),
      ),
    );
  }
}
