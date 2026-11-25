import 'package:material_ui/material_ui.dart';

import '../../../core/design/app_colors.dart';
import '../../../core/design/app_icon.dart';
import '../../../core/design/tokens.g.dart';
import '../../../shared/ui/app_button.dart';
import '../../../shared/ui/app_card.dart';
import '../../../shared/ui/app_notice.dart';
import 'tenant_text.dart';

/// Why the complex cannot be entered.
enum BlockedTenantKind { unavailable, notFound }

/// Skeleton of the status card while the complex is checked.
class TenantCheckingView extends StatelessWidget {
  const TenantCheckingView({super.key});

  @override
  Widget build(BuildContext context) {
    return const _StatusLayout(
      child: AppCard(
        isLoading: true,
        semanticLabel: TenantText.checking,
        child: SizedBox.shrink(),
      ),
    );
  }
}

/// The check failed (no connection or an unexpected answer). It is not «not
/// available»: the player can retry.
class TenantCheckFailedView extends StatelessWidget {
  const TenantCheckFailedView({super.key, required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return _StatusLayout(
      child: AppNotice(
        tone: AppNoticeTone.error,
        message: TenantText.checkFailed,
        action: AppButton(
          label: TenantText.retry,
          variant: AppButtonVariant.text,
          onPressed: onRetry,
        ),
      ),
    );
  }
}

/// «Complejo no disponible» or «Complejo no encontrado» (Pantallas
/// PLATODO-26 · pantallas 1 y 2). Both look alike so «does not exist» is not
/// told apart from «not available» more than needed; only «not found» makes
/// the way out the primary action.
class TenantBlockedView extends StatelessWidget {
  const TenantBlockedView({
    super.key,
    required this.kind,
    required this.onGoToPlatform,
  });

  final BlockedTenantKind kind;

  /// «Ir a Playtodoo»: back to the start or search of the app.
  final VoidCallback onGoToPlatform;

  static const double _iconSize = AppTokens.space7;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final mutedColor = Theme.of(context).colorScheme.onSurfaceVariant;
    final content = _BlockedContent.of(kind);

    return _StatusLayout(
      child: AppCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: AppTokens.space2),
            AppIcon(content.icon, size: _iconSize, color: mutedColor),
            const SizedBox(height: AppTokens.space4),
            Semantics(
              header: true,
              container: true,
              child: Text(
                content.title,
                textAlign: TextAlign.center,
                style: textTheme.headlineMedium,
              ),
            ),
            const SizedBox(height: AppTokens.space3),
            Text(
              content.text,
              textAlign: TextAlign.center,
              style: textTheme.bodyMedium?.copyWith(color: mutedColor),
            ),
            const SizedBox(height: AppTokens.space5),
            AppButton(
              label: TenantText.goToPlatform,
              variant: content.actionVariant,
              onPressed: onGoToPlatform,
            ),
          ],
        ),
      ),
    );
  }
}

class _BlockedContent {
  const _BlockedContent({
    required this.icon,
    required this.title,
    required this.text,
    required this.actionVariant,
  });

  factory _BlockedContent.of(BlockedTenantKind kind) => switch (kind) {
    BlockedTenantKind.unavailable => const _BlockedContent(
      icon: AppIcons.lock,
      title: TenantText.unavailableTitle,
      text: TenantText.unavailableText,
      actionVariant: AppButtonVariant.secondary,
    ),
    BlockedTenantKind.notFound => const _BlockedContent(
      icon: AppIcons.search,
      title: TenantText.notFoundTitle,
      text: TenantText.notFoundText,
      actionVariant: AppButtonVariant.primary,
    ),
  };

  final AppIcons icon;
  final String title;
  final String text;
  final AppButtonVariant actionVariant;
}

/// Brand name over a centered column on `surface-alt`: full width with a
/// 16 margin on phones, at most [_maxWidth] on wider screens.
class _StatusLayout extends StatelessWidget {
  const _StatusLayout({required this.child});

  /// [POR DEFINIR: token de ancho de las tarjetas de estado], 480 per
  /// Pantallas PLATODO-26.
  static const double _maxWidth = 480;

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: context.appColors.surfaceAlt,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(AppTokens.space4),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: _maxWidth),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    TenantText.brand,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.headlineSmall
                        ?.copyWith(color: context.appColors.brandNavy),
                  ),
                  const SizedBox(height: AppTokens.space4),
                  child,
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
