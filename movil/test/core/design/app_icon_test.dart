import 'package:flutter_test/flutter_test.dart';
import 'package:playtodoo/core/design/app_icon.dart';

import '../../support/themed.dart';

void main() {
  testWidgets('a labelled icon is announced', (tester) async {
    final handle = tester.ensureSemantics();
    await pumpThemed(
      tester,
      const AppIcon(AppIcons.lock, semanticLabel: 'Ocupado'),
    );

    expect(find.bySemanticsLabel('Ocupado'), findsOneWidget);
    handle.dispose();
  });

  testWidgets('an unlabelled icon is decorative and excluded', (tester) async {
    final handle = tester.ensureSemantics();
    await pumpThemed(tester, const AppIcon(AppIcons.lock));

    expect(find.semantics.byLabel(RegExp('.+')), findsNothing);
    handle.dispose();
  });
}
