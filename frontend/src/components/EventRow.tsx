import { Link } from 'react-router'
import type { EventSummary } from '../api/types'
import { useI18n } from '../i18n/I18nContext'

/** One line of the programme: date block, title and venue, price, how full the house is. */
export function EventRow({ event }: { event: EventSummary }) {
  const { t, f } = useI18n()
  const { day, month, weekday, time } = f.dateParts(event.startsAt)
  const soldShare = event.totalSeats === 0 ? 0 : 1 - event.availableSeats / event.totalSeats
  const soldOut = event.availableSeats === 0
  const fewLeft = !soldOut && event.availableSeats <= Math.max(10, event.totalSeats * 0.1)

  return (
    <li>
      <Link
        to={`/events/${event.id}`}
        className="group grid grid-cols-[4.5rem_1fr] items-center gap-x-5 gap-y-3 rounded-2xl px-3 py-5 transition-colors hover:bg-card sm:grid-cols-[5.5rem_1fr_auto] sm:px-5"
      >
        <div className="text-center leading-none">
          <div className="font-display text-5xl font-extrabold text-ink">{day}</div>
          <div className="mt-1 text-sm font-semibold">{month}</div>
          <div className="mt-0.5 text-xs text-ink-soft">
            {weekday} {time}
          </div>
        </div>

        <div className="min-w-0">
          <p className="text-sm text-ink-soft">{t.category[event.category]}</p>
          <h3 className="mt-0.5 text-3xl font-bold group-hover:text-curtain sm:text-[2.1rem]">{event.title}</h3>
          <p className="mt-1 text-ink-soft">
            {event.venueName}, {event.city}
          </p>
        </div>

        <div className="col-span-2 flex items-center justify-between gap-6 sm:col-span-1 sm:flex-col sm:items-end sm:justify-center sm:gap-2">
          <div className="sm:text-right">
            <p className="text-sm text-ink-soft">{t.events.from}</p>
            <p className="font-display text-3xl font-bold leading-none">{f.money(event.priceFrom)}</p>
          </div>
          <div className="w-40">
            <div className="h-1.5 overflow-hidden rounded-full bg-line" aria-hidden>
              <div className="h-full rounded-full bg-chart-sold" style={{ width: `${Math.round(soldShare * 100)}%` }} />
            </div>
            <p className={`mt-1 text-xs sm:text-right ${soldOut || fewLeft ? 'font-semibold text-curtain' : 'text-ink-soft'}`}>
              {soldOut ? t.events.soldOut : fewLeft ? t.events.onlyLeft(event.availableSeats) : t.events.left(event.availableSeats)}
            </p>
          </div>
        </div>
      </Link>
    </li>
  )
}
