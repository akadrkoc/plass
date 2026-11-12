import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { api } from '../api/endpoints'
import type { Booking } from '../api/types'
import { useCountdown } from '../components/Countdown'
import { ApiErrorNotice, PageLoader } from '../components/Feedback'
import { TicketStub } from '../components/TicketStub'
import { useI18n } from '../i18n/I18nContext'
import { useApi } from '../lib/useApi'

export function CheckoutPage() {
  const bookingId = Number(useParams().bookingId)
  const booking = useApi(`booking:${bookingId}`, () => api.bookings.get(bookingId))

  if (booking.loading && !booking.data) return <PageLoader />
  if (booking.error) return <ApiErrorNotice error={booking.error} onRetry={booking.reload} />
  if (!booking.data) return null

  if (booking.data.status === 'CONFIRMED') return <Confirmed booking={booking.data} />
  if (booking.data.status !== 'PENDING') return <Closed booking={booking.data} />
  return <Payment booking={booking.data} onChange={booking.setData} />
}

function Payment({ booking, onChange }: { booking: Booking; onChange: (booking: Booking) => void }) {
  const { t, f, errorText } = useI18n()
  const navigate = useNavigate()
  const [busy, setBusy] = useState<'pay' | 'release' | null>(null)
  const [error, setError] = useState<string | null>(null)
  const countdown = useCountdown(booking.expiresAt)

  const holdMs = new Date(booking.expiresAt).getTime() - new Date(booking.createdAt).getTime()
  const share = holdMs > 0 ? countdown.remainingMs / holdMs : 0

  async function pay() {
    setBusy('pay')
    setError(null)
    try {
      onChange(await api.bookings.pay(booking.id))
    } catch (e) {
      setError(errorText(e, t.checkout.paymentFailed))
      setBusy(null)
    }
  }

  async function release() {
    setBusy('release')
    try {
      await api.bookings.cancel(booking.id)
    } finally {
      navigate(`/events/${booking.event.id}`)
    }
  }

  if (countdown.expired) {
    return <Closed booking={{ ...booking, status: 'EXPIRED' }} />
  }

  return (
    <div className="mx-auto grid max-w-5xl gap-8 lg:grid-cols-[1fr_22rem]">
      <section aria-labelledby="checkout-title" className="space-y-6">
        <div>
          <h1 id="checkout-title" className="text-6xl font-extrabold">
            {t.checkout.title}
          </h1>
          <p className="mt-2 text-ink-soft">{t.checkout.intro}</p>
        </div>

        <div className="rounded-3xl border border-line bg-card p-6" role="timer" aria-live="off">
          <div className="flex items-baseline justify-between gap-4">
            <span className="font-display text-7xl font-extrabold tabular-nums text-curtain">{countdown.text}</span>
            <span className="text-right text-ink-soft">{t.checkout.leftToPay}</span>
          </div>
          <div className="mt-4 h-2 overflow-hidden rounded-full bg-line" aria-hidden>
            <div className="h-full rounded-full bg-curtain transition-[width] duration-1000 ease-linear" style={{ width: `${share * 100}%` }} />
          </div>
        </div>

        <div className="rounded-3xl border border-line bg-card p-6">
          <h2 className="text-3xl font-bold">{booking.event.title}</h2>
          <p className="mt-1 font-semibold">{f.longDate(booking.event.startsAt)}</p>
          <p className="text-ink-soft">
            {booking.event.venueName}, {booking.event.city}
          </p>
          <ul className="mt-5 divide-y divide-line border-y border-line">
            {booking.seats.map((seat) => (
              <li key={seat.seatId} className="flex justify-between py-2.5">
                <span>
                  {t.common.seatName(seat.row, seat.number)}
                  <span className="text-ink-soft">, {t.tier[seat.tier]}</span>
                </span>
                <span>{f.money(seat.price)}</span>
              </li>
            ))}
          </ul>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="font-semibold">{t.common.total}</span>
            <span className="font-display text-4xl font-bold">{f.money(booking.totalAmount)}</span>
          </div>
        </div>
      </section>

      <aside className="h-max rounded-3xl bg-ink p-6 text-white lg:sticky lg:top-24">
        <h2 className="text-3xl font-bold">{t.checkout.payment}</h2>
        <p className="mt-3 text-white/75">{t.checkout.demoNote}</p>
        {error && (
          <p role="alert" className="mt-4 rounded-lg bg-curtain px-3 py-2 text-sm">
            {error}
          </p>
        )}
        <button type="button" className="btn btn-light mt-6 w-full" onClick={pay} disabled={busy !== null}>
          {busy === 'pay' ? t.checkout.paying : t.checkout.pay(f.money(booking.totalAmount))}
        </button>
        <button
          type="button"
          className="mt-3 w-full rounded-full py-2 text-sm font-semibold text-white/75 hover:text-white hover:underline"
          onClick={release}
          disabled={busy !== null}
        >
          {t.checkout.release}
        </button>
        <p className="mt-6 text-sm text-white/60">{t.checkout.reference(booking.reference)}</p>
      </aside>
    </div>
  )
}

function Confirmed({ booking }: { booking: Booking }) {
  const { t } = useI18n()
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-6xl font-extrabold">{t.checkout.confirmedTitle}</h1>
        <p className="mt-2 text-ink-soft">{t.checkout.confirmedBody}</p>
      </div>
      <TicketStub booking={booking} />
      <div className="flex flex-wrap gap-3">
        <Link to="/tickets" className="btn btn-primary">
          {t.checkout.goToTickets}
        </Link>
        <Link to="/" className="btn btn-quiet">
          {t.checkout.findAnother}
        </Link>
      </div>
    </div>
  )
}

function Closed({ booking }: { booking: Booking }) {
  const { t } = useI18n()
  const expired = booking.status === 'EXPIRED'
  return (
    <div className="mx-auto max-w-xl py-12 text-center">
      <h1 className="text-5xl font-extrabold">{expired ? t.checkout.releasedTitle : t.checkout.cancelledTitle}</h1>
      <p className="mt-3 text-ink-soft">{expired ? t.checkout.releasedBody : t.checkout.cancelledBody}</p>
      <Link to={`/events/${booking.event.id}`} className="btn btn-primary mt-8">
        {t.checkout.chooseAgain}
      </Link>
    </div>
  )
}
