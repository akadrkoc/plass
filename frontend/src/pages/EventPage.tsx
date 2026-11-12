import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { api } from '../api/endpoints'
import type { Seat, SeatTier } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ApiErrorNotice, ErrorNotice, PageLoader } from '../components/Feedback'
import { SeatLegend, SeatMap } from '../components/SeatMap'
import { useI18n } from '../i18n/I18nContext'
import { useApi } from '../lib/useApi'

const MAX_SEATS = 6
const REFRESH_MS = 10_000
const PENDING_KEY = 'plass.pendingSelection'

export function EventPage() {
  const eventId = Number(useParams().id)
  const navigate = useNavigate()
  const { user } = useAuth()
  const { t, f, errorText } = useI18n()

  const event = useApi(`event:${eventId}`, () => api.events.get(eventId))
  const seats = useApi(`seats:${eventId}`, () => api.events.seats(eventId))

  const [selected, setSelected] = useState<Set<number>>(() => restoreSelection(eventId))
  const [notice, setNotice] = useState<string | null>(null)
  const [reserving, setReserving] = useState(false)

  // The saved selection (see restoreSelection) is only meant to survive one trip to the login page.
  useEffect(() => sessionStorage.removeItem(PENDING_KEY), [])

  // Keep the map fresh so seats other people take show up without a reload.
  const reloadSeats = seats.reload
  useEffect(() => {
    const timer = window.setInterval(reloadSeats, REFRESH_MS)
    return () => window.clearInterval(timer)
  }, [reloadSeats])

  const prices = useMemo(() => {
    const map = { VIP: 0, PREMIUM: 0, STANDARD: 0 } as Record<SeatTier, number>
    event.data?.prices.forEach((p) => (map[p.tier] = p.price))
    return map
  }, [event.data])

  if (event.loading && !event.data) return <PageLoader />
  if (event.error) return <ApiErrorNotice error={event.error} onRetry={event.reload} />
  if (!event.data) return null
  const show = event.data

  // Seats someone else booked since we picked them drop out of the selection automatically.
  const chosen = (seats.data ?? []).filter((seat) => selected.has(seat.id) && seat.status === 'AVAILABLE')
  const lost = (seats.data ?? []).filter((seat) => selected.has(seat.id) && seat.status !== 'AVAILABLE')
  const chosenIds = new Set(chosen.map((seat) => seat.id))
  const total = chosen.reduce((sum, seat) => sum + prices[seat.tier], 0)
  const available = seats.data ? seats.data.filter((seat) => seat.status === 'AVAILABLE').length : show.availableSeats

  function toggle(seat: Seat) {
    setNotice(null)
    const next = new Set(chosenIds)
    if (next.has(seat.id)) {
      next.delete(seat.id)
    } else if (next.size >= MAX_SEATS) {
      setNotice(t.event.maxSeats(MAX_SEATS))
      return
    } else {
      next.add(seat.id)
    }
    setSelected(next)
  }

  async function reserve() {
    if (!user) {
      sessionStorage.setItem(PENDING_KEY, JSON.stringify({ eventId, seatIds: [...chosenIds] }))
      navigate(`/login?next=${encodeURIComponent(`/events/${eventId}`)}`)
      return
    }
    setReserving(true)
    setNotice(null)
    try {
      const booking = await api.bookings.hold(eventId, [...chosenIds])
      navigate(`/checkout/${booking.id}`)
    } catch (error) {
      if (error instanceof ApiError && error.code === 'SEATS_UNAVAILABLE') {
        setNotice(t.event.justTaken(t.common.joinList(error.seats), error.seats.length))
        seats.reload()
      } else {
        setNotice(errorText(error, t.event.reserveFailed))
      }
      setReserving(false)
    }
  }

  return (
    <article className={`space-y-8 ${chosen.length > 0 ? 'pb-20 lg:pb-0' : ''}`}>
      <Link to="/" className="text-sm font-semibold text-ink-soft hover:text-ink hover:underline">
        {t.event.allEvents}
      </Link>

      <header className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-[minmax(0,1fr)_20rem] lg:items-end">
        <div>
          <p className="text-ink-soft">{t.category[show.category]}</p>
          <h1 className="mt-1 text-6xl font-extrabold sm:text-7xl">{show.title}</h1>
          <p className="mt-4 text-lg font-semibold">{f.longDate(show.startsAt)}</p>
          <p className="text-ink-soft">
            {show.venue.name}, {show.venue.address}, {show.venue.city}
          </p>
        </div>
        <p className="max-w-prose text-ink-soft">{show.description}</p>
      </header>

      {show.status === 'CANCELLED' && <ErrorNotice message={t.event.cancelled} />}

      <div className="grid grid-cols-[minmax(0,1fr)] gap-8 lg:grid-cols-[minmax(0,1fr)_20rem] lg:items-start">
        <section aria-labelledby="seats-title" className="rounded-3xl border border-line bg-card p-5 sm:p-8">
          <div className="mb-6 flex flex-wrap items-baseline justify-between gap-2">
            <h2 id="seats-title" className="text-3xl font-bold">
              {t.event.chooseSeats}
            </h2>
            <p className="text-sm text-ink-soft">{t.event.available(available, show.totalSeats)}</p>
          </div>
          {seats.data ? (
            <SeatMap
              seats={seats.data}
              prices={prices}
              selected={chosenIds}
              onToggle={toggle}
              disabled={!show.bookable}
            />
          ) : seats.error ? (
            <ApiErrorNotice error={seats.error} onRetry={seats.reload} />
          ) : (
            <PageLoader label={t.event.loadingSeatMap} />
          )}
          <div className="mt-8 border-t border-line pt-5">
            <SeatLegend prices={prices} />
          </div>
        </section>

        <aside aria-labelledby="summary-title" className="rounded-3xl bg-ink p-6 text-white lg:sticky lg:top-24">
          <h2 id="summary-title" className="text-3xl font-bold">
            {t.event.yourSeats}
          </h2>
          {!show.bookable ? (
            <p className="mt-3 text-white/75">{t.event.closed}</p>
          ) : chosen.length === 0 ? (
            <p className="mt-3 text-white/75">{t.event.empty(MAX_SEATS)}</p>
          ) : (
            <ul className="mt-4 divide-y divide-white/15">
              {chosen.map((seat) => (
                <li key={seat.id} className="flex items-center justify-between gap-3 py-2.5">
                  <div>
                    <p className="font-semibold">{t.common.seatName(seat.row, seat.number)}</p>
                    <p className="text-sm text-white/65">{t.tier[seat.tier]}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span>{f.money(prices[seat.tier])}</span>
                    <button
                      type="button"
                      onClick={() => toggle(seat)}
                      className="rounded-full px-2 text-white/70 hover:bg-white/10 hover:text-white"
                      aria-label={t.event.remove(seat.row, seat.number)}
                    >
                      ✕
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}

          {chosen.length > 0 && (
            <div className="mt-4 flex items-baseline justify-between border-t border-white/25 pt-4">
              <span>{t.common.total}</span>
              <span className="font-display text-4xl font-bold">{f.money(total)}</span>
            </div>
          )}

          {lost.length > 0 && !notice && (
            <p role="alert" className="mt-4 rounded-lg bg-brass/25 px-3 py-2 text-sm text-white">
              {t.event.lostSeats(t.common.joinList(lost.map(label)), lost.length)}
            </p>
          )}
          {notice && (
            <p role="alert" className="mt-4 rounded-lg bg-brass/25 px-3 py-2 text-sm text-white">
              {notice}
            </p>
          )}

          <button
            type="button"
            className="btn btn-light mt-5 w-full"
            disabled={chosen.length === 0 || reserving || !show.bookable}
            onClick={reserve}
          >
            {reserving
              ? t.event.reserving
              : chosen.length === 0
                ? t.event.reserveSeats
                : t.event.reserveCount(chosen.length)}
          </button>
          <p className="mt-3 text-sm text-white/65">{user ? t.event.holdNote : t.event.loginNote}</p>
        </aside>
      </div>

      {/* On small screens the summary sits below the map, so keep the total and the button in reach. */}
      {chosen.length > 0 && show.bookable && (
        <div className="fixed inset-x-0 bottom-0 z-20 border-t border-white/15 bg-ink px-5 py-3 text-white lg:hidden">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-4">
            <div>
              <p className="text-sm text-white/70">{t.common.seatCount(chosen.length)}</p>
              <p className="font-display text-2xl font-bold leading-none">{f.money(total)}</p>
            </div>
            <button type="button" className="btn btn-light" disabled={reserving} onClick={reserve}>
              {reserving ? t.event.reserving : t.event.reserve}
            </button>
          </div>
        </div>
      )}
    </article>
  )
}

function label(seat: Seat) {
  return `${seat.row}${seat.number}`
}

/** Restores seats picked before the visitor was sent to the login page. */
function restoreSelection(eventId: number): Set<number> {
  try {
    const saved = JSON.parse(sessionStorage.getItem(PENDING_KEY) ?? 'null') as { eventId: number; seatIds: number[] } | null
    if (saved?.eventId === eventId) return new Set(saved.seatIds)
  } catch {
    // ignore unreadable storage
  }
  return new Set()
}
