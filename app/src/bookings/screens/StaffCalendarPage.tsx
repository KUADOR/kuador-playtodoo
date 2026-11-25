import { useId, useMemo, useState } from 'react'
import { addDays, Alert, Button, Field, formatLongDate } from '../../shared/ui'
import type { IsoDate } from '../../shared/ui'
import type { StaffBooking, StaffBookingsApi } from '../api/staffBookingsApi'
import { BlockSlotDialog } from '../components/BlockSlotDialog'
import { BookingDetailDialog } from '../components/BookingDetailDialog'
import { CancelBookingDialog } from '../components/CancelBookingDialog'
import { ManualBookingDialog } from '../components/ManualBookingDialog'
import type { SlotDraft } from '../model/slotDraft'
import { StaffSchedule } from '../components/StaffSchedule'
import type { Court } from '../model/court'
import { buildSchedule } from '../model/staffSchedule'
import type { FreeRow } from '../model/staffSchedule'
import { STAFF_TEXT } from '../model/staffText'
import { useStaffDay } from './useStaffDay'
import styles from '../components/StaffCalendar.module.css'

export interface StaffCalendarPageProps {
  api: StaffBookingsApi
  /** Courts of the complex, at least one. */
  courts: Court[]
  /** IANA time zone of the complex ([POR DEFINIR] in PRODUCT.md). */
  timeZone: string
  /** Local date of the complex the page opens on; the page never reads the clock. */
  today: IsoDate
}

type OpenDialog =
  | { type: 'manual'; slot: SlotDraft }
  | { type: 'block'; slot: SlotDraft }
  | { type: 'detail'; booking: StaffBooking }
  | { type: 'cancel'; booking: StaffBooking }
  | null

const DAY_STEP = 1

/** Staff calendar of the complex (Pantallas · Pantalla 6): see the day of a court, book, block and cancel. */
export function StaffCalendarPage({ api, courts, timeZone, today }: StaffCalendarPageProps) {
  const [courtId, setCourtId] = useState(courts[0].id)
  const [date, setDate] = useState(today)
  const [dialog, setDialog] = useState<OpenDialog>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const dayTitleId = useId()
  const court = courts.find(({ id }) => id === courtId) ?? courts[0]
  const day = useStaffDay(api, court, date, timeZone)
  const rows = useMemo(() => buildSchedule(court, date, day.bookings, timeZone), [court, date, day.bookings, timeZone])

  const slotOf = (row: FreeRow): SlotDraft => ({ courtId: court.id, date, start: row.start })
  const close = () => setDialog(null)
  const openDialog = (next: Exclude<OpenDialog, null>) => {
    setNotice(null)
    setDialog(next)
  }
  const finish = (message: string) => () => {
    setDialog(null)
    setNotice(message)
    day.reload()
  }

  return (
    <main className={styles.page}>
      <h1 className={styles.pageTitle}>{STAFF_TEXT.pageTitle}</h1>

      <div className={styles.filters}>
        <Field
          kind="select"
          label={STAFF_TEXT.court}
          options={courts.map(({ id, name }) => ({ value: id, label: name }))}
          value={court.id}
          onChange={(event) => setCourtId(event.target.value)}
        />
        <Field
          kind="text"
          type="date"
          label={STAFF_TEXT.day}
          value={date}
          onChange={(event) => event.target.value && setDate(event.target.value)}
        />
        <div className={styles.dayNavigation}>
          <Button variant="secondary" label={STAFF_TEXT.previousDay} onClick={() => setDate(addDays(date, -DAY_STEP))} />
          <Button variant="secondary" label={STAFF_TEXT.nextDay} onClick={() => setDate(addDays(date, DAY_STEP))} />
        </div>
      </div>

      <section className={styles.day} aria-labelledby={dayTitleId} aria-busy={day.status === 'loading'}>
        <h2 id={dayTitleId} className={styles.dayTitle} aria-live="polite">
          {court.name} · {formatLongDate(date)}
        </h2>
        {notice && (
          <Alert variant="success" title={notice} />
        )}
        <StaffSchedule
          status={day.status}
          rows={rows}
          onRetry={day.reload}
          onNewBooking={(row) => openDialog({ type: 'manual', slot: slotOf(row) })}
          onBlockSlot={(row) => openDialog({ type: 'block', slot: slotOf(row) })}
          onViewBooking={(booking) => openDialog({ type: 'detail', booking })}
          onCancelBooking={(booking) => openDialog({ type: 'cancel', booking })}
        />
      </section>

      {dialog?.type === 'manual' && (
        <ManualBookingDialog api={api} courts={courts} timeZone={timeZone} initialSlot={dialog.slot} onSaved={finish(STAFF_TEXT.bookingSaved)} onClose={close} />
      )}
      {dialog?.type === 'block' && (
        <BlockSlotDialog
          api={api}
          courts={courts}
          timeZone={timeZone}
          initialSlot={dialog.slot}
          onSaved={finish(STAFF_TEXT.blockSaved)}
          onBookingCancelled={day.reload}
          onClose={close}
        />
      )}
      {dialog?.type === 'detail' && (
        <BookingDetailDialog
          booking={dialog.booking}
          courts={courts}
          timeZone={timeZone}
          onCancelBooking={(booking) => setDialog({ type: 'cancel', booking })}
          onClose={close}
        />
      )}
      {dialog?.type === 'cancel' && (
        <CancelBookingDialog api={api} booking={dialog.booking} onCancelled={finish(STAFF_TEXT.entryCancelled)} onClose={close} />
      )}
    </main>
  )
}
