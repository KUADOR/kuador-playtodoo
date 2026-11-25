import { Component } from '@angular/core';
import { BookingCalendar } from '../../shared/ui/booking-calendar/booking-calendar';
import { CalendarStatus } from '../../shared/ui/booking-calendar/booking-calendar.types';
import {
  CalendarFixture,
  CALENDAR_FIXTURES,
} from '../../shared/ui/booking-calendar/calendar-fixtures';
import { Button, ButtonVariant } from '../../shared/ui/button/button';
import { Card, CardVariant } from '../../shared/ui/card/card';
import { Field, FieldOption } from '../../shared/ui/field/field';

interface ButtonExample {
  readonly variant: ButtonVariant;
  readonly name: string;
  readonly label: string;
}

interface CalendarExample {
  readonly name: string;
  readonly source: CalendarFixture;
  readonly status: CalendarStatus;
}

interface CardExample {
  readonly variant: CardVariant;
  readonly name: string;
  readonly title: string;
  readonly detail: string;
}

/**
 * Development-only catalog with every state of the base components.
 * It is the source of the visual evidence (screenshots) for the design system.
 */
@Component({
  selector: 'app-design-catalog',
  imports: [Button, Field, Card, BookingCalendar],
  templateUrl: './design-catalog.html',
  styleUrl: './design-catalog.css',
})
export class DesignCatalog {
  protected readonly buttons: readonly ButtonExample[] = [
    { variant: 'primary', name: 'Primario', label: 'Guardar cambios' },
    { variant: 'secondary', name: 'Secundario', label: 'Ver complejo' },
    { variant: 'danger', name: 'Peligro', label: 'Suspender complejo' },
    { variant: 'text', name: 'Texto', label: 'Cancelar' },
  ];

  protected readonly planOptions: readonly FieldOption[] = [
    { value: 'basic', label: 'Básico' },
    { value: 'pro', label: 'Profesional' },
  ];

  protected readonly cards: readonly CardExample[] = [
    {
      variant: 'complex',
      name: 'Complejo',
      title: 'Canchas El Sol',
      detail: '4 canchas · suscripción activa',
    },
    { variant: 'court', name: 'Cancha', title: 'Cancha 2 · Fútbol 7', detail: 'Césped sintético' },
    {
      variant: 'booking',
      name: 'Reserva',
      title: 'Sábado 18:00 – 19:00',
      detail: 'Cancha 2 · confirmada',
    },
    {
      variant: 'tournament',
      name: 'Torneo',
      title: 'Copa de verano',
      detail: '8 equipos inscritos',
    },
    {
      variant: 'news',
      name: 'Novedad',
      title: 'Nuevas luces LED',
      detail: 'Publicada hace 2 días',
    },
  ];

  protected readonly calendars: readonly CalendarExample[] = [
    { name: 'Semana', source: CALENDAR_FIXTURES.weekReadonly, status: 'ready' },
    { name: 'Día · franjas de 30 min', source: CALENDAR_FIXTURES.day30, status: 'ready' },
    { name: 'Día · franjas de 60 min', source: CALENDAR_FIXTURES.day60, status: 'ready' },
    { name: 'Día · franjas de 90 min', source: CALENDAR_FIXTURES.day90, status: 'ready' },
    { name: 'Mes', source: CALENDAR_FIXTURES.monthMixed, status: 'ready' },
    { name: 'Día sin horarios', source: CALENDAR_FIXTURES.dayEmpty, status: 'ready' },
    { name: 'Cargando', source: CALENDAR_FIXTURES.weekReadonly, status: 'loading' },
    { name: 'Error', source: CALENDAR_FIXTURES.weekReadonly, status: 'error' },
  ];
}
