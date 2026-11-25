import { useCallback, useEffect, useState } from 'react'
import type { IsoDate } from '../../shared/ui'
import type { StaffBooking, StaffBookingsApi } from '../api/staffBookingsApi'
import { bookingErrorCodeOf } from '../api/staffBookingsApi'
import type { Court } from '../model/court'
import { dayQuery } from '../model/staffSchedule'
import type { ScheduleStatus } from '../components/StaffSchedule'

interface StaffDayState {
  /** Court and day the data belongs to. */
  key: string
  status: ScheduleStatus
  bookings: StaffBooking[]
}

/**
 * Loads the active entries of one court and local day. A newer request always wins over an older one.
 * `reload` refreshes the same day in place, so the rows (and the focus on them) stay while it loads.
 */
export function useStaffDay(api: Pick<StaffBookingsApi, 'listBookings'>, court: Court, date: IsoDate, timeZone: string) {
  const key = `${court.id}/${date}`
  const [state, setState] = useState<StaffDayState>({ key, status: 'loading', bookings: [] })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let current = true
    api.listBookings(dayQuery(court, date, timeZone)).then(
      (bookings) => current && setState({ key, status: 'ready', bookings }),
      (error: unknown) => {
        bookingErrorCodeOf(error)
        if (current) setState({ key, status: 'error', bookings: [] })
      },
    )
    return () => {
      current = false
    }
  }, [api, court, date, timeZone, key, version])

  const reload = useCallback(() => {
    // After an error the retry shows the skeleton again; a refresh of loaded data keeps the rows.
    setState((previous) => (previous.status === 'error' ? { ...previous, status: 'loading' } : previous))
    setVersion((value) => value + 1)
  }, [])
  const status: ScheduleStatus = state.key === key ? state.status : 'loading'
  return { status, bookings: state.key === key ? state.bookings : [], reload }
}
