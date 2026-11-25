import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/app_icon.dart';
import 'package:playtodoo/core/design/tokens.g.dart';
import 'package:playtodoo/shared/ui/app_card.dart';

import '../../support/themed.dart';

void main() {
  const content = 'Cancha 1 · Fútbol 5';
  const skeletonKey = ValueKey('app-card-skeleton');
  final checkIcon = find.byWidgetPredicate(
    (w) => w is AppIcon && w.icon == AppIcons.check,
  );

  BoxDecoration cardDecoration(WidgetTester tester) {
    return tester.widget<Ink>(find.byType(Ink)).decoration! as BoxDecoration;
  }

  testWidgets('normal: surface, border and card shadow, no check', (
    tester,
  ) async {
    await pumpThemed(tester, const AppCard(child: Text(content)));

    final decoration = cardDecoration(tester);
    expect(decoration.color, AppTokens.colorSurface);
    expect(decoration.boxShadow, AppTokens.shadowCard);
    expect((decoration.border! as Border).top.color, AppTokens.colorBorder);
    expect(checkIcon, findsNothing);
  });

  testWidgets(
    'selected: primary border of 2, tint, check and selected semantics',
    (tester) async {
      final handle = tester.ensureSemantics();
      await pumpThemed(
        tester,
        AppCard(
          selected: true,
          semanticLabel: content,
          onTap: () {},
          child: const Text(content),
        ),
      );

      final decoration = cardDecoration(tester);
      final border = decoration.border! as Border;
      expect(decoration.color, AppTokens.colorPrimaryTint);
      expect(border.top.color, AppTokens.colorPrimary);
      expect(border.top.width, AppTokens.borderWidthLg);
      expect(checkIcon, findsOneWidget);
      expect(
        tester.getSemantics(find.byType(AppCard)),
        isSemantics(label: content, isSelected: true, isButton: true),
      );
      handle.dispose();
    },
  );

  testWidgets('loading: skeleton replaces the content and ignores taps', (
    tester,
  ) async {
    var taps = 0;
    await pumpThemed(
      tester,
      AppCard(isLoading: true, onTap: () => taps++, child: const Text(content)),
    );

    expect(find.byKey(skeletonKey), findsOneWidget);
    expect(find.text(content), findsNothing);
    await tester.tap(find.byType(AppCard));
    expect(taps, 0);
  });

  testWidgets('loading: skeleton pulses by default', (tester) async {
    await pumpThemed(
      tester,
      const AppCard(isLoading: true, child: Text(content)),
    );

    final opacity = tester
        .widget<FadeTransition>(
          find.descendant(
            of: find.byKey(skeletonKey),
            matching: find.byType(FadeTransition),
          ),
        )
        .opacity;
    final start = opacity.value;
    await tester.pump(const Duration(milliseconds: 300));
    expect(opacity.value, isNot(start));
  });

  testWidgets('loading: skeleton stays still when animations are disabled', (
    tester,
  ) async {
    await pumpThemed(
      tester,
      const AppCard(isLoading: true, child: Text(content)),
      disableAnimations: true,
    );

    final opacity = tester
        .widget<FadeTransition>(
          find.descendant(
            of: find.byKey(skeletonKey),
            matching: find.byType(FadeTransition),
          ),
        )
        .opacity;
    final start = opacity.value;
    await tester.pump(const Duration(milliseconds: 300));
    expect(opacity.value, start);
    expect(tester.hasRunningAnimations, isFalse);
  });

  testWidgets('interactive: calls onTap and meets the touch guidelines', (
    tester,
  ) async {
    final handle = tester.ensureSemantics();
    var taps = 0;
    await pumpThemed(
      tester,
      AppCard(
        semanticLabel: content,
        onTap: () => taps++,
        child: const Text(content),
      ),
    );

    await tester.tap(find.byType(AppCard));

    expect(taps, 1);
    expect(
      tester.getSize(find.byType(AppCard)).height,
      greaterThanOrEqualTo(AppTokens.sizeTouchMin),
    );
    await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
    await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));
    await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
    handle.dispose();
  });

  testWidgets(
    'selected interactive card fills its width, check beside content',
    (tester) async {
      const width = 300.0;
      await pumpThemed(
        tester,
        SizedBox(
          width: width,
          child: AppCard(
            selected: true,
            onTap: () {},
            child: const Text(content),
          ),
        ),
      );

      expect(tester.getSize(find.byType(AppCard)).width, width);
      expect(
        tester.getTopRight(find.text(content)).dx,
        lessThanOrEqualTo(tester.getTopLeft(checkIcon).dx),
      );
    },
  );
}
