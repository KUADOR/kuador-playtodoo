import { useState } from 'react'
import type { ReactNode } from 'react'
import { BookingCalendar, Button, Card, Field } from '../../shared/ui'
import type { ButtonVariant, CalendarDayInput, CardVariant, FieldOption, SlotSelection } from '../../shared/ui'
import styles from './DesignCatalog.module.css'

const BUTTON_VARIANTS: { variant: ButtonVariant; name: string; label: string }[] = [
  { variant: 'primary', name: 'Primario', label: 'Reservar cancha' },
  { variant: 'secondary', name: 'Secundario', label: 'Ver horarios' },
  { variant: 'danger', name: 'Peligro', label: 'Cancelar reserva' },
  { variant: 'text', name: 'Texto', label: 'Ver detalle' },
]

const COURT_OPTIONS: FieldOption[] = [
  { value: 'court-1', label: 'Cancha 1' },
  { value: 'court-2', label: 'Cancha 2' },
]

const CARD_VARIANTS: { variant: CardVariant; title: string; detail: string }[] = [
  { variant: 'complex', title: 'Canchas El Sol', detail: 'Complejo · 4 canchas' },
  { variant: 'court', title: 'Cancha 1', detail: 'Fútbol 5 · césped sintético' },
  { variant: 'booking', title: 'Viernes 20:00', detail: 'Reserva · Cancha 1' },
  { variant: 'tournament', title: 'Torneo de verano', detail: 'Torneo · 8 equipos' },
  { variant: 'news', title: 'Nuevos horarios', detail: 'Novedad · abrimos a las 7:00' },
]

const SAMPLE_ERROR = 'Ese horario ya fue reservado. Elige otro disponible.'

const CALENDAR_TODAY = '2026-12-02'
const CALENDAR_DAYS: CalendarDayInput[] = [
  { date: '2026-11-30', slots: [{ start: '18:00', status: 'available' }, { start: '19:00', status: 'occupied' }, { start: '20:00', status: 'available' }] },
  { date: '2026-12-01', slots: [] },
  { date: '2026-12-02', slots: [{ start: '18:00', status: 'occupied' }, { start: '19:00', status: 'available' }, { start: '20:00', status: 'available' }] },
  { date: '2026-12-04', slots: [{ start: '19:00', status: 'available' }, { start: '20:00', status: 'occupied' }, { start: '21:00', status: 'available' }] },
  { date: '2026-12-05', slots: [{ start: '09:00', status: 'available' }, { start: '10:00', status: 'available' }, { start: '11:00', status: 'occupied' }] },
  { date: '2026-12-06', slots: [{ start: '09:00', status: 'occupied' }, { start: '10:00', status: 'occupied' }] },
]

/** Development-only catalog with every state of the base components (source of the PR screenshots). */
export default function DesignCatalog() {
  return (
    <main className={styles.page}>
      <h1>Catálogo de componentes</h1>
      <p className={styles.hint}>
        Solo en desarrollo. Hover, foco y pulsado se ven al pasar el mouse, usar Tab o pulsar cada componente.
      </p>

      <CatalogSection title="Botón">
        {BUTTON_VARIANTS.map(({ variant, name, label }) => (
          <div key={variant} className={styles.row}>
            <h3 className={styles.rowTitle}>{name}</h3>
            <div className={styles.states}>
              <State name="Normal">
                <Button variant={variant} label={label} />
              </State>
              <State name="Cargando">
                <Button variant={variant} label={label} loading />
              </State>
              <State name="Deshabilitado">
                <Button variant={variant} label={label} disabled />
              </State>
            </div>
          </div>
        ))}
      </CatalogSection>

      <CatalogSection title="Campo">
        <div className={styles.grid}>
          <Field kind="text" label="Nombre del complejo" placeholder="Ej.: Canchas El Sol" />
          <Field kind="text" label="Nombre del complejo" defaultValue="Canchas El Sol" help="Así lo verán los jugadores." />
          <Field kind="text" label="Horario" defaultValue="20:00" error={SAMPLE_ERROR} />
          <Field kind="text" label="Nombre del complejo" defaultValue="Canchas El Sol" disabled />
          <Field kind="select" label="Cancha" placeholder="Elige una cancha" options={COURT_OPTIONS} defaultValue="" />
          <Field kind="select" label="Cancha" options={COURT_OPTIONS} defaultValue="court-1" />
          <Field kind="select" label="Cancha" options={COURT_OPTIONS} defaultValue="court-2" error={SAMPLE_ERROR} />
          <Field kind="select" label="Cancha" options={COURT_OPTIONS} defaultValue="court-1" disabled />
        </div>
      </CatalogSection>

      <CatalogSection title="Tarjeta">
        <div className={styles.grid}>
          {CARD_VARIANTS.map(({ variant, title, detail }) => (
            <Card key={variant} variant={variant}>
              <CardContent title={title} detail={detail} />
            </Card>
          ))}
          <Card variant="booking" selected>
            <CardContent title="Viernes 20:00" detail="Seleccionada" />
          </Card>
          <Card variant="complex" loading />
        </div>
      </CatalogSection>

      <CatalogSection title="Calendario de reservas">
        <CalendarSamples />
      </CatalogSection>
    </main>
  )
}

function CalendarSamples() {
  const [selected, setSelected] = useState<SlotSelection | null>({ date: CALENDAR_TODAY, start: '19:00' })
  const common = { slotDurationMinutes: 60, days: CALENDAR_DAYS, today: CALENDAR_TODAY }

  return (
    <div className={styles.stack}>
      <State name="Editable · semana">
        <BookingCalendar {...common} selected={selected} onSelectionChange={setSelected} />
      </State>
      <State name="Editable · día">
        <BookingCalendar {...common} initialView="day" selected={selected} onSelectionChange={setSelected} />
      </State>
      <State name="Editable · mes">
        <BookingCalendar {...common} initialView="month" selected={selected} onSelectionChange={setSelected} />
      </State>
      <State name="Modo lectura">
        <BookingCalendar {...common} mode="readonly" />
      </State>
      <State name="Cargando">
        <BookingCalendar {...common} status="loading" />
      </State>
      <State name="Error">
        <BookingCalendar {...common} status="error" />
      </State>
    </div>
  )
}

function CatalogSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className={styles.section} aria-label={title}>
      <h2>{title}</h2>
      {children}
    </section>
  )
}

function State({ name, children }: { name: string; children: ReactNode }) {
  return (
    <div className={styles.state}>
      <span className={styles.stateName}>{name}</span>
      {children}
    </div>
  )
}

function CardContent({ title, detail }: { title: string; detail: string }) {
  return (
    <>
      <h3 className={styles.cardTitle}>{title}</h3>
      <p className={styles.cardDetail}>{detail}</p>
    </>
  )
}
