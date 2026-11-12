import { QRCodeSVG } from 'qrcode.react'
import type { ReactNode } from 'react'
import type { Booking } from '../api/types'
import { useI18n } from '../i18n/I18nContext'
import type { Messages } from '../i18n/messages'

/** A printed ticket: event details on the left, a tear-off stub with the QR code on the right. */
export function TicketStub({ booking, actions }: { booking: Booking; actions?: ReactNode }) {
  const { t, f } = useI18n()
  const void_ = booking.status === 'CANCELLED' || booking.status === 'EXPIRED'
  const stamp = stampText(booking, t)

  return (
    <article className={`ticket ${void_ ? 'text-ink-soft' : ''}`} aria-label={t.ticket.label(booking.reference)}>
      <div className="relative min-w-0 p-5 sm:p-6">
        {stamp && <span className={`stamp ${void_ ? 'text-curtain' : 'text-brass'}`}>{stamp}</span>}
        <p className="text-sm">{t.category[booking.event.category]}</p>
        <h3 className={`mt-0.5 pr-24 text-3xl font-bold sm:text-4xl ${void_ ? 'line-through decoration-2' : ''}`}>
          {booking.event.title}
        </h3>
        <p className="mt-2 font-semibold">{f.longDate(booking.event.startsAt)}</p>
        <p className="text-ink-soft">
          {booking.event.venueName}, {booking.event.city}
        </p>

        <dl className="mt-4 flex flex-wrap gap-x-8 gap-y-3">
          <div>
            <dt className="text-sm text-ink-soft">{t.ticket.seats}</dt>
            <dd className="font-display text-2xl font-bold">
              {booking.seats.map((seat) => `${seat.row}${seat.number}`).join(', ')}
            </dd>
          </div>
          <div>
            <dt className="text-sm text-ink-soft">{t.ticket.section}</dt>
            <dd className="font-display text-2xl font-bold">
              {[...new Set(booking.seats.map((seat) => t.tier[seat.tier]))].join(', ')}
            </dd>
          </div>
          <div>
            <dt className="text-sm text-ink-soft">{paymentLabel(booking, t)}</dt>
            <dd className="font-display text-2xl font-bold">{f.money(booking.totalAmount)}</dd>
          </div>
        </dl>
        {actions && <div className="mt-5 flex flex-wrap items-center gap-3">{actions}</div>}
      </div>

      <div className="ticket-stub flex w-32 flex-col items-center justify-center gap-2 p-4 sm:w-40">
        <div className={void_ || booking.status === 'PENDING' ? 'opacity-25' : undefined}>
          <QRCodeSVG value={booking.reference} size={96} fgColor="#2a1838" bgColor="transparent" />
        </div>
        <p className="font-display text-lg font-bold tracking-wider">{booking.reference}</p>
      </div>
    </article>
  )
}

function stampText(booking: Booking, t: Messages) {
  switch (booking.status) {
    case 'CANCELLED':
      return t.ticket.stampCancelled
    case 'EXPIRED':
      return t.ticket.stampExpired
    case 'PENDING':
      return t.ticket.stampNotPaid
    default:
      return undefined
  }
}

function paymentLabel(booking: Booking, t: Messages) {
  if (booking.status === 'CONFIRMED') return t.ticket.paid
  if (booking.status === 'CANCELLED' && booking.confirmedAt) return t.ticket.refunded
  return t.ticket.toPay
}
