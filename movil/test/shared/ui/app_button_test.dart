import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/tokens.g.dart';
import 'package:playtodoo/shared/ui/app_button.dart';

import '../../support/themed.dart';

void main() {
  const label = 'Reservar cancha';

  for (final variant in AppButtonVariant.values) {
    group('$variant', () {
      testWidgets('meets the minimum touch size', (tester) async {
        await pumpThemed(
          tester,
          AppButton(label: label, variant: variant, onPressed: () {}),
        );

        final size = tester.getSize(find.byType(AppButton));
        expect(size.width, greaterThanOrEqualTo(AppTokens.sizeTouchMin));
        expect(size.height, greaterThanOrEqualTo(AppTokens.sizeTouchMin));
      });

      testWidgets('meets Android, iOS, label and contrast guidelines', (
        tester,
      ) async {
        final handle = tester.ensureSemantics();
        await pumpThemed(
          tester,
          AppButton(label: label, variant: variant, onPressed: () {}),
        );

        await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
        await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));
        await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
        await expectLater(tester, meetsGuideline(textContrastGuideline));
        handle.dispose();
      });

      testWidgets('keeps the variant colors while loading', (tester) async {
        Material buttonMaterial() => tester.widget<Material>(
          find
              .descendant(
                of: find.byType(AppButton),
                matching: find.byType(Material),
              )
              .first,
        );

        await pumpThemed(
          tester,
          AppButton(label: label, variant: variant, onPressed: () {}),
        );
        final idle = buttonMaterial();

        await pumpThemed(
          tester,
          AppButton(
            label: label,
            variant: variant,
            isLoading: true,
            onPressed: () {},
          ),
        );
        final loading = buttonMaterial();

        expect(loading.color, idle.color);
        expect(loading.textStyle?.color, idle.textStyle?.color);
        expect(loading.shape, idle.shape);
      });

      testWidgets('calls onPressed when tapped', (tester) async {
        var taps = 0;
        await pumpThemed(
          tester,
          AppButton(label: label, variant: variant, onPressed: () => taps++),
        );

        await tester.tap(find.text(label));

        expect(taps, 1);
      });
    });
  }

  testWidgets('disabled: ignores taps and is exposed as disabled', (
    tester,
  ) async {
    final handle = tester.ensureSemantics();
    await pumpThemed(tester, const AppButton(label: label, onPressed: null));

    await tester.tap(find.text(label), warnIfMissed: false);

    expect(
      tester.getSemantics(find.text(label)),
      matchesSemantics(
        label: label,
        isButton: true,
        hasEnabledState: true,
        isEnabled: false,
        isFocusable: false,
        hasTapAction: false,
      ),
    );
    handle.dispose();
  });

  testWidgets('loading: shows spinner and loading label, ignores taps', (
    tester,
  ) async {
    final handle = tester.ensureSemantics();
    var taps = 0;
    await pumpThemed(
      tester,
      AppButton(label: label, isLoading: true, onPressed: () => taps++),
    );

    await tester.tap(find.byType(AppButton), warnIfMissed: false);

    expect(taps, 0);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    expect(find.text(AppButton.defaultLoadingLabel), findsOneWidget);
    expect(find.text(label), findsNothing);
    expect(
      find.bySemanticsLabel(AppButton.defaultLoadingLabel),
      findsOneWidget,
    );
    handle.dispose();
  });

  testWidgets('loading with reduced motion: static three-quarter arc', (
    tester,
  ) async {
    await pumpThemed(
      tester,
      AppButton(label: label, isLoading: true, onPressed: () {}),
      disableAnimations: true,
    );

    final spinner = tester.widget<CircularProgressIndicator>(
      find.byType(CircularProgressIndicator),
    );
    expect(spinner.value, AppButton.reducedMotionSpinnerValue);
  });

  testWidgets('loading with motion: spinning indicator', (tester) async {
    await pumpThemed(
      tester,
      AppButton(label: label, isLoading: true, onPressed: () {}),
    );

    final spinner = tester.widget<CircularProgressIndicator>(
      find.byType(CircularProgressIndicator),
    );
    expect(spinner.value, isNull);
  });

  testWidgets('shows the focus ring only with keyboard focus', (tester) async {
    FocusManager.instance.highlightStrategy =
        FocusHighlightStrategy.alwaysTraditional;
    addTearDown(
      () => FocusManager.instance.highlightStrategy =
          FocusHighlightStrategy.automatic,
    );
    await pumpThemed(tester, AppButton(label: label, onPressed: () {}));
    expect(find.byKey(const ValueKey('app-focus-ring')), findsNothing);

    await tester.sendKeyEvent(LogicalKeyboardKey.tab);
    await tester.pump();

    expect(find.byKey(const ValueKey('app-focus-ring')), findsOneWidget);
  });
}
