import 'package:material_ui/material_ui.dart';

import '../../app/app_routes.dart';
import '../../core/design/tokens.g.dart';
import '../../shared/ui/app_button.dart';

/// Startup screen until the player features exist.
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key, required this.showDesignCatalogLink});

  final bool showDesignCatalogLink;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(AppTokens.space4),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'Playtodoo',
                  style: Theme.of(context).textTheme.displaySmall,
                ),
                if (showDesignCatalogLink) ...[
                  const SizedBox(height: AppTokens.space5),
                  AppButton(
                    label: 'Ver catálogo de diseño',
                    variant: AppButtonVariant.secondary,
                    onPressed: () =>
                        Navigator.of(context)
                            .pushNamed(AppRoutes.designCatalog),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
