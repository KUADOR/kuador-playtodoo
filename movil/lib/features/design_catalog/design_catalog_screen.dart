import 'package:material_ui/material_ui.dart';

import '../../core/design/app_icon.dart';
import '../../core/design/tokens.g.dart';
import '../../shared/ui/app_button.dart';
import '../../shared/ui/app_card.dart';
import '../../shared/ui/app_field.dart';
import '../../shared/ui/booking_calendar/booking_calendar.dart';

/// Debug-only gallery of the base components in every state.
class DesignCatalogScreen extends StatefulWidget {
  const DesignCatalogScreen({super.key});

  @override
  State<DesignCatalogScreen> createState() => _DesignCatalogScreenState();
}

class _DesignCatalogScreenState extends State<DesignCatalogScreen> {
  static const List<AppSelectOption<String>> _sportOptions = [
    AppSelectOption(value: 'futbol', label: 'Fútbol'),
    AppSelectOption(value: 'padel', label: 'Pádel'),
    AppSelectOption(value: 'tenis', label: 'Tenis'),
  ];

  static const String _sampleToday = '2026-12-04';
  static const int _sampleSlotMinutes = 60;
  static const List<CalendarDay> _sampleDays = [
    CalendarDay(
      date: '2026-12-03',
      slots: [
        CalendarSlot(start: '19:00', availability: SlotAvailability.available),
        CalendarSlot(start: '20:00', availability: SlotAvailability.occupied),
      ],
    ),
    CalendarDay(
      date: _sampleToday,
      slots: [
        CalendarSlot(start: '17:00', availability: SlotAvailability.available),
        CalendarSlot(start: '18:00', availability: SlotAvailability.occupied),
        CalendarSlot(start: '19:00', availability: SlotAvailability.available),
        CalendarSlot(start: '20:00', availability: SlotAvailability.available),
        CalendarSlot(start: '21:00', availability: SlotAvailability.occupied),
        CalendarSlot(start: '22:00', availability: SlotAvailability.available),
      ],
    ),
    CalendarDay(date: '2026-12-05', slots: []),
  ];

  String? _sport;
  bool _isCardSelected = true;
  SlotSelection? _calendarSelection = const SlotSelection(
    date: _sampleToday,
    start: '19:00',
  );

  void _noop() {}

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Catálogo de diseño')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppTokens.space4),
          children: [
            _Section(
              title: 'Botones',
              children: [
                AppButton(label: 'Reservar cancha', onPressed: _noop),
                AppButton(
                  label: 'Ver horarios',
                  variant: AppButtonVariant.secondary,
                  leadingIcon: AppIcons.clock,
                  onPressed: _noop,
                ),
                AppButton(
                  label: 'Cancelar reserva',
                  variant: AppButtonVariant.danger,
                  onPressed: _noop,
                ),
                AppButton(
                  label: 'Ver más',
                  variant: AppButtonVariant.text,
                  onPressed: _noop,
                ),
                const AppButton(label: 'Reservar cancha', onPressed: null),
                const AppButton(
                  label: 'Ver horarios',
                  variant: AppButtonVariant.secondary,
                  onPressed: null,
                ),
                AppButton(
                  label: 'Guardar cambios',
                  isLoading: true,
                  onPressed: _noop,
                ),
              ],
            ),
            _Section(
              title: 'Campos',
              children: [
                const AppField.text(
                  label: 'Nombre',
                  helperText: 'Como aparece en tu documento',
                ),
                const AppField.text(
                  label: 'Correo',
                  keyboardType: TextInputType.emailAddress,
                  errorText: 'Ese correo no es válido. Revisa que tenga @.',
                ),
                const AppField.text(label: 'Complejo', enabled: false),
                AppField<String>.select(
                  label: 'Deporte',
                  options: _sportOptions,
                  value: _sport,
                  onChanged: (sport) => setState(() => _sport = sport),
                ),
                AppField<String>.select(
                  label: 'Cancha',
                  options: _sportOptions,
                  errorText: 'Elige una cancha para continuar.',
                  onChanged: (_) {},
                ),
                const AppField<String>.select(
                  label: 'Horario',
                  options: _sportOptions,
                  enabled: false,
                ),
              ],
            ),
            _Section(
              title: 'Tarjetas',
              children: [
                AppCard(
                  semanticLabel: 'Cancha 1, fútbol 5',
                  selected: _isCardSelected,
                  onTap: () =>
                      setState(() => _isCardSelected = !_isCardSelected),
                  child: Text(
                    'Cancha 1 · Fútbol 5',
                    style: textTheme.titleMedium,
                  ),
                ),
                AppCard(
                  child: Text(
                    'Complejo Los Andes',
                    style: textTheme.titleMedium,
                  ),
                ),
                const AppCard(isLoading: true, child: SizedBox.shrink()),
              ],
            ),
            _Section(
              title: 'Calendario',
              children: [
                BookingCalendar(
                  slotDurationMinutes: _sampleSlotMinutes,
                  days: _sampleDays,
                  today: _sampleToday,
                  selected: _calendarSelection,
                  onSelectionChange: (selection) =>
                      setState(() => _calendarSelection = selection),
                ),
                const BookingCalendar(
                  slotDurationMinutes: _sampleSlotMinutes,
                  days: _sampleDays,
                  today: _sampleToday,
                  initialView: CalendarView.week,
                  mode: CalendarMode.readonly,
                ),
                const BookingCalendar(
                  slotDurationMinutes: _sampleSlotMinutes,
                  days: _sampleDays,
                  today: _sampleToday,
                  initialView: CalendarView.month,
                ),
                const BookingCalendar(
                  slotDurationMinutes: _sampleSlotMinutes,
                  days: [],
                  today: _sampleToday,
                  status: CalendarStatus.loading,
                ),
                BookingCalendar(
                  slotDurationMinutes: _sampleSlotMinutes,
                  days: const [],
                  today: _sampleToday,
                  status: CalendarStatus.error,
                  onRetry: _noop,
                ),
              ],
            ),
            _Section(
              title: 'Iconos',
              children: [
                Wrap(
                  spacing: AppTokens.space4,
                  children: [
                    for (final icon in AppIcons.values)
                      AppIcon(icon, semanticLabel: icon.name),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.children});

  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppTokens.space6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(title, style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: AppTokens.space3),
          for (final child in children)
            Padding(
              padding: const EdgeInsets.only(bottom: AppTokens.space3),
              child: child,
            ),
        ],
      ),
    );
  }
}
