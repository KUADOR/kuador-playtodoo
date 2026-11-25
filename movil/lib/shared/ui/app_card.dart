import 'package:material_ui/material_ui.dart';

import '../../core/design/app_colors.dart';
import '../../core/design/app_icon.dart';
import '../../core/design/tokens.g.dart';
import 'app_focus_ring.dart';

/// Base card the domain cards (complex, court, booking…) build on.
///
/// Selected: primary border, primary tint and a check icon, announced as
/// selected. Loading: a skeleton replaces [child]; it stays still when the
/// platform asks to reduce motion. A [semanticLabel] replaces the semantics
/// of [child].
class AppCard extends StatelessWidget {
  const AppCard({
    super.key,
    required this.child,
    this.selected = false,
    this.isLoading = false,
    this.onTap,
    this.semanticLabel,
  });

  final Widget child;
  final bool selected;
  final bool isLoading;
  final VoidCallback? onTap;
  final String? semanticLabel;

  static final BorderRadius _radius = BorderRadius.circular(AppTokens.radiusLg);

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    final scheme = Theme.of(context).colorScheme;
    final border = selected
        ? Border.all(color: scheme.primary, width: AppTokens.borderWidthLg)
        : Border.all(
            color: scheme.outlineVariant,
            width: AppTokens.borderWidthSm,
          );

    final body = Padding(
      padding: const EdgeInsets.all(AppTokens.space4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: isLoading
                ? const _CardSkeleton()
                : ExcludeSemantics(
                    excluding: semanticLabel != null,
                    child: child,
                  ),
          ),
          if (selected) ...[
            const SizedBox(width: AppTokens.space2),
            AppIcon(AppIcons.check, color: scheme.primary),
          ],
        ],
      ),
    );

    Widget card = Material(
      type: MaterialType.transparency,
      child: Ink(
        decoration: BoxDecoration(
          color: selected ? colors.primaryTint : scheme.surface,
          borderRadius: _radius,
          border: border,
          boxShadow: AppTokens.shadowCard,
        ),
        child: InkWell(
          onTap: isLoading ? null : onTap,
          borderRadius: _radius,
          child: ConstrainedBox(
            constraints: const BoxConstraints(
              minHeight: AppTokens.sizeTouchMin,
            ),
            child: body,
          ),
        ),
      ),
    );

    if (onTap != null) {
      card = AppFocusRing(borderRadius: _radius, child: card);
    }

    return Semantics(
      container: true,
      label: semanticLabel,
      selected: selected,
      button: onTap != null,
      child: card,
    );
  }
}

class _CardSkeleton extends StatefulWidget {
  const _CardSkeleton();

  @override
  State<_CardSkeleton> createState() => _CardSkeletonState();
}

class _CardSkeletonState extends State<_CardSkeleton>
    with SingleTickerProviderStateMixin {
  static const Duration _pulseDuration = Duration(milliseconds: 900);
  static const double _minOpacity = 0.4;
  static const List<double> _lineWidthFactors = [0.6, 0.9, 0.4];
  static const String _loadingLabel = 'Cargando';

  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: _pulseDuration,
    lowerBound: _minOpacity,
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (MediaQuery.disableAnimationsOf(context)) {
      _controller
        ..stop()
        ..value = _controller.upperBound;
    } else if (!_controller.isAnimating) {
      _controller.repeat(reverse: true);
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final lineColor = context.appColors.disabledBackground;
    return Semantics(
      label: _loadingLabel,
      child: FadeTransition(
        key: const ValueKey('app-card-skeleton'),
        opacity: _controller,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (final factor in _lineWidthFactors)
              Padding(
                padding: const EdgeInsets.only(bottom: AppTokens.space2),
                child: FractionallySizedBox(
                  widthFactor: factor,
                  child: Container(
                    height: AppTokens.space3,
                    decoration: BoxDecoration(
                      color: lineColor,
                      borderRadius: BorderRadius.circular(AppTokens.radiusMd),
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
