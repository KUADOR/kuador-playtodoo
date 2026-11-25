import 'package:material_ui/material_ui.dart';

void main() {
  runApp(const PlaytodooApp());
}

class PlaytodooApp extends StatelessWidget {
  const PlaytodooApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      title: 'Playtodoo',
      home: Scaffold(body: Center(child: Text('Playtodoo'))),
    );
  }
}
