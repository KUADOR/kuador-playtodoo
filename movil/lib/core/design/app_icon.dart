import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:material_ui/material_ui.dart';

/// Icons the app uses. Widgets depend on this enum, never on the icon library,
/// so the library can change without touching them.
enum AppIcons {
  alert(LucideIcons.circleAlert),
  calendar(LucideIcons.calendar),
  check(LucideIcons.check),
  chevronDown(LucideIcons.chevronDown),
  chevronLeft(LucideIcons.chevronLeft),
  chevronRight(LucideIcons.chevronRight),
  clock(LucideIcons.clock),
  eye(LucideIcons.eye),
  lock(LucideIcons.lock),
  mapPin(LucideIcons.mapPin),
  search(LucideIcons.search);

  const AppIcons(this.data);

  final IconData data;
}

/// Adapter over the Lucide icon font.
///
/// With [semanticLabel] the icon is announced by screen readers; without it
/// the icon is decorative and excluded from the semantics tree.
class AppIcon extends StatelessWidget {
  const AppIcon(
    this.icon, {
    super.key,
    this.semanticLabel,
    this.size,
    this.color,
  });

  final AppIcons icon;
  final String? semanticLabel;
  final double? size;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final glyph = Icon(
      icon.data,
      size: size,
      color: color,
      semanticLabel: semanticLabel,
    );
    return semanticLabel == null ? ExcludeSemantics(child: glyph) : glyph;
  }
}
