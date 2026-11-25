import 'package:material_ui/material_ui.dart';

import '../../core/design/app_colors.dart';
import '../../core/design/tokens.g.dart';

/// Loading placeholder: pulsing lines announced with [label].
///
/// It stays still when the platform asks to reduce motion.
class AppSkeleton extends StatefulWidget {
  const AppSkeleton({
    super.key,
    required this.label,
    required this.lineWidthFactors,
    this.lineHeight = AppTokens.space3,
  });

  final String label;

  /// One line per value, as a fraction of the available width.
  final List<double> lineWidthFactors;
  final double lineHeight;

  @override
  State<AppSkeleton> createState() => _AppSkeletonState();
}

class _AppSkeletonState extends State<AppSkeleton>
    with SingleTickerProviderStateMixin {
  static const Duration _pulseDuration = Duration(milliseconds: 900);
  static const double _minOpacity = 0.4;

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
      label: widget.label,
      child: FadeTransition(
        opacity: _controller,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (final factor in widget.lineWidthFactors)
              Padding(
                padding: const EdgeInsets.only(bottom: AppTokens.space2),
                child: FractionallySizedBox(
                  widthFactor: factor,
                  child: Container(
                    height: widget.lineHeight,
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
