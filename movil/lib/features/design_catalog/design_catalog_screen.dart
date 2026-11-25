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

  static const List<_ButtonSample> _buttonSamples = [
    _ButtonSample(
      name: 'Primario',
      label: 'Reservar cancha',
      variant: AppButtonVariant.primary,
    ),
    _ButtonSample(
      name: 'Secundario',
      label: 'Ver horarios',
      variant: AppButtonVariant.secondary,
      leadingIcon: AppIcons.clock,
    ),
    _ButtonSample(
      name: 'Peligro',
      label: 'Cancelar reserva',
      variant: AppButtonVariant.danger,
    ),
    _ButtonSample(
      name: 'Texto',
      label: 'Ver detalle',
      variant: AppButtonVariant.text,
    ),
  ];

  static const List<_CardSample> _cardSamples = [
    _CardSample(title: 'Canchas El Sol', detail: 'Complejo · 4 canchas'),
    _CardSample(title: 'Cancha 1', detail: 'Fútbol 5 · césped sintético'),
    _CardSample(title: 'Viernes 20:00', detail: 'Reserva · Cancha 1'),
    _CardSample(title: 'Torneo de verano', detail: 'Torneo · 8 equipos'),
    _CardSample(
      title: 'Nuevos horarios',
      detail: 'Novedad · abrimos a las 7:00',
    ),
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
    return Scaffold(
      appBar: AppBar(title: const Text('Catálogo de diseño')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppTokens.space4),
          children: [
            _Section(
              title: 'Botones',
              children: [
                for (final sample in _buttonSamples) ...[
                  _StateLabel(sample.name),
                  AppButton(
                    label: sample.label,
                    variant: sample.variant,
                    leadingIcon: sample.leadingIcon,
                    onPressed: _noop,
                  ),
                  AppButton(
                    label: sample.label,
                    variant: sample.variant,
                    isLoading: true,
                    onPressed: _noop,
                  ),
                  AppButton(
                    label: sample.label,
                    variant: sample.variant,
                    onPressed: null,
                  ),
                ],
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
                for (final sample in _cardSamples)
                  AppCard(
                    child: _CardContent(
                      title: sample.title,
                      detail: sample.detail,
                    ),
                  ),
                AppCard(
                  semanticLabel: 'Viernes 20:00, reserva en Cancha 1',
                  selected: _isCardSelected,
                  onTap: () =>
                      setState(() => _isCardSelected = !_isCardSelected),
                  child: const _CardContent(
                    title: 'Viernes 20:00',
                    detail: 'Seleccionada',
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

class _ButtonSample {
  const _ButtonSample({
    required this.name,
    required this.label,
    required this.variant,
    this.leadingIcon,
  });

  final String name;
  final String label;
  final AppButtonVariant variant;
  final AppIcons? leadingIcon;
}

/// One card per domain variant (complex, court, booking, tournament, news).
class _CardSample {
  const _CardSample({required this.title, required this.detail});

  final String title;
  final String detail;
}

class _StateLabel extends StatelessWidget {
  const _StateLabel(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(text, style: Theme.of(context).textTheme.titleSmall);
  }
}

class _CardContent extends StatelessWidget {
  const _CardContent({required this.title, required this.detail});

  final String title;
  final String detail;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: textTheme.titleMedium),
        Text(
          detail,
          style: textTheme.bodySmall?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}
