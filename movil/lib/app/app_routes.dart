import 'package:flutter/foundation.dart';
import 'package:material_ui/material_ui.dart';

import '../features/booking/booking_preview_screen.dart';
import '../features/design_catalog/design_catalog_screen.dart';
import '../features/home/home_screen.dart';
import '../features/tenant/tenant_entry_screen.dart';
import '../features/tenant/tenant_preview_screen.dart';

abstract final class AppRoutes {
  static const String home = '/';
  static const String designCatalog = '/design-catalog';
  static const String bookingPreview = '/booking-preview';
  static const String tenantPreview = '/tenant-preview';
  static const String tenantEntry = '/tenant-entry';
}

/// Named routes of the app. The design catalog and the previews against the
/// contract mocks (booking flow and complex states) and the entry to a complex
/// of the real api by its subdomain only exist when
/// [enableDesignCatalog] is true, which by default means debug builds.
Map<String, WidgetBuilder> buildAppRoutes({
  bool enableDesignCatalog = kDebugMode,
}) {
  return {
    AppRoutes.home: (_) =>
        HomeScreen(showDesignCatalogLink: enableDesignCatalog),
    if (enableDesignCatalog)
      AppRoutes.designCatalog: (_) => const DesignCatalogScreen(),
    if (enableDesignCatalog)
      AppRoutes.bookingPreview: (_) => const BookingPreviewScreen(),
    if (enableDesignCatalog)
      AppRoutes.tenantPreview: (_) => const TenantPreviewScreen(),
    if (enableDesignCatalog)
      AppRoutes.tenantEntry: (_) => const TenantEntryScreen(),
  };
}
