import 'package:flutter_test/flutter_test.dart';
import 'package:playtodoo/main.dart';

void main() {
  testWidgets('starts the app', (tester) async {
    await tester.pumpWidget(const PlaytodooApp());

    expect(find.text('Playtodoo'), findsOneWidget);
  });
}
