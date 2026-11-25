import 'package:flutter/foundation.dart';
import 'package:material_ui/material_ui.dart';

import 'app/app_routes.dart';
import 'core/design/app_theme.dart';

void main() {
  runApp(const PlaytodooApp());
}

class PlaytodooApp extends StatelessWidget {
  const PlaytodooApp({super.key, this.enableDesignCatalog = kDebugMode});

  final bool enableDesignCatalog;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Playtodoo',
      theme: AppTheme.light(),
      initialRoute: AppRoutes.home,
      routes: buildAppRoutes(enableDesignCatalog: enableDesignCatalog),
    );
  }
}
