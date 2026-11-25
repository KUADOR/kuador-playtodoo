import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/app_colors.dart';
import 'package:playtodoo/core/design/app_icon.dart';
import 'package:playtodoo/core/design/app_theme.dart';
import 'package:playtodoo/core/design/tokens.g.dart';
import 'package:playtodoo/shared/ui/app_notice.dart';

import '../../support/themed.dart';

const String _openLabel = 'Abrir aviso';

Future<void> _pumpSheetLauncher(
  WidgetTester tester,
  ValueChanged<String?> onResult,
) {
  return tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Builder(
        builder: (context) => Scaffold(
          body: Center(
            child: TextButton(
              onPressed: () async => onResult(
                await showAppNoticeSheet<String>(
                  context: context,
                  tone: AppNoticeTone.warning,
                  title: 'Título',
                  message: 'Mensaje',
                  actions: const [
                    AppNoticeAction(label: 'Primera', value: 'first'),
                    AppNoticeAction(label: 'Segunda', value: 'second'),
                  ],
                ),
              ),
              child: const Text(_openLabel),
            ),
          ),
        ),
      ),
    ),
  );
}

void main() {
  testWidgets('the inline notice uses the tone icon and a 1.5 px border of '
      'its color', (tester) async {
    await pumpThemed(
      tester,
      const AppNotice(tone: AppNoticeTone.error, message: 'Algo falló'),
    );

    expect(find.byIcon(AppIcons.alert.data), findsOneWidget);
    final decoration =
        tester
                .widget<DecoratedBox>(
                  find
                      .ancestor(
                        of: find.text('Algo falló'),
                        matching: find.byType(DecoratedBox),
                      )
                      .last,
                )
                .decoration
            as BoxDecoration;
    final border = decoration.border! as Border;
    expect(border.top.color, AppColors.light.danger);
    expect(border.top.width, AppTokens.borderWidthMd);
  });

  testWidgets('the sheet closes with the value of the chosen action', (
    tester,
  ) async {
    String? result;
    await _pumpSheetLauncher(tester, (value) => result = value);

    await tester.tap(find.text(_openLabel));
    await tester.pumpAndSettle();
    expect(find.byIcon(AppIcons.warning.data), findsOneWidget);
    expect(find.text('Título'), findsOneWidget);

    await tester.tap(find.text('Segunda'));
    await tester.pumpAndSettle();
    expect(result, 'second');
    expect(find.text('Título'), findsNothing);
  });

  testWidgets('Esc closes the sheet with no value and focus goes back to the '
      'opener', (tester) async {
    String? result = 'unset';
    await _pumpSheetLauncher(tester, (value) => result = value);
    await tester.sendKeyEvent(LogicalKeyboardKey.tab);
    await tester.pump();
    expect(Focus.of(tester.element(find.text(_openLabel))).hasFocus, isTrue);

    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.pumpAndSettle();
    expect(find.text('Título'), findsOneWidget);

    await tester.sendKeyEvent(LogicalKeyboardKey.escape);
    await tester.pumpAndSettle();
    expect(result, isNull);
    expect(find.text('Título'), findsNothing);
    expect(Focus.of(tester.element(find.text(_openLabel))).hasFocus, isTrue);
  });
}
