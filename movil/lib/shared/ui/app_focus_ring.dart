import 'package:material_ui/material_ui.dart';

import '../../core/design/app_colors.dart';
import '../../core/design/tokens.g.dart';

/// Draws the brand focus ring around [child] while it, or a descendant,
/// has keyboard focus (traditional highlight mode, like `:focus-visible`).
///
/// Ring: `border-focus-ring` wide, `border-focus-offset` away from the child,
/// in `color-focus` with a `color-focus-halo` halo.
class AppFocusRing extends StatefulWidget {
  const AppFocusRing({
    super.key,
    required this.child,
    required this.borderRadius,
  });

  final Widget child;
  final BorderRadius borderRadius;

  @override
  State<AppFocusRing> createState() => _AppFocusRingState();
}

class _AppFocusRingState extends State<AppFocusRing> {
  static const double _outset =
      AppTokens.borderFocusOffset + AppTokens.borderFocusRing;

  bool _hasFocus = false;

  @override
  void initState() {
    super.initState();
    FocusManager.instance.addHighlightModeListener(_onHighlightModeChanged);
  }

  @override
  void dispose() {
    FocusManager.instance.removeHighlightModeListener(_onHighlightModeChanged);
    super.dispose();
  }

  void _onHighlightModeChanged(FocusHighlightMode mode) => setState(() {});

  void _onFocusChanged(bool hasFocus) => setState(() => _hasFocus = hasFocus);

  bool get _isRingVisible =>
      _hasFocus &&
      FocusManager.instance.highlightMode == FocusHighlightMode.traditional;

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    return Focus(
      canRequestFocus: false,
      skipTraversal: true,
      onFocusChange: _onFocusChanged,
      child: Stack(
        fit: StackFit.passthrough,
        clipBehavior: Clip.none,
        children: [
          widget.child,
          if (_isRingVisible)
            Positioned.fill(
              left: -_outset,
              top: -_outset,
              right: -_outset,
              bottom: -_outset,
              child: IgnorePointer(
                child: DecoratedBox(
                  key: const ValueKey('app-focus-ring'),
                  decoration: BoxDecoration(
                    borderRadius:
                        widget.borderRadius + BorderRadius.circular(_outset),
                    border: Border.all(
                      color: colors.focus,
                      width: AppTokens.borderFocusRing,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: colors.focusHalo,
                        spreadRadius: AppTokens.borderFocusRing,
                      ),
                    ],
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
