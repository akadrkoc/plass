import { useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import { api } from '../api/endpoints'
import type { Booking } from '../api/types'
import { ApiErrorNotice, EmptyState, PageLoader } from '../components/Feedback'
import { TicketStub } from '../components/TicketStub'
import { useI18n } from '../i18n/I18nContext'
import { useApi, useNow } from '../lib/useApi'

export function MyTicketsPage() {
  const { t } = useI18n()
  const bookings = useApi('my-bookings', () => api.bookings.mine())
  const now = useNow()

  if (bookings.loading && !bookings.data) return <PageLoader label={t.myTickets.loading} />
  if (bookings.error) return <ApiErrorNotice error={bookings.error} onRetry={bookings.reload} />

  const all = bookings.data ?? []
  const awaitingPayment = all.filter((b) => b.status === 'PENDING' && new Date(b.expiresAt).getTime() > now)
  const upcoming = all
    .filter((b) => b.status === 'CONFIRMED' && new Date(b.event.startsAt).getTime() > now)
    .sort((a, b) => a.event.startsAt.localeCompare(b.event.startsAt))
  const history = all.filter((b) => !awaitingPayment.includes(b) && !upcoming.includes(b))

  function replace(updated: Booking) {
    bookings.setData(all.map((b) => (b.id === updated.id ? updated : b)))
  }

  return (
    <div className="space-y-12">
      <div>
        <h1 className="text-6xl font-extrabold">{t.myTickets.title}</h1>
        <p className="mt-2 text-ink-soft">{t.myTickets.intro}</p>
      </div>

      {all.length === 0 && (
        <EmptyState title={t.myTickets.emptyTitle}>
          <p>{t.myTickets.emptyBody}</p>
          <Link to="/" className="btn btn-primary mt-6">
            {t.myTickets.seeWhatsOn}
          </Link>
        </EmptyState>
      )}

      {awaitingPayment.length > 0 && (
        <TicketSection title={t.myTickets.awaitingPayment}>
          {awaitingPayment.map((booking) => (
            <TicketStub
              key={booking.id}
              booking={booking}
              actions={
                <Link to={`/checkout/${booking.id}`} className="btn btn-primary">
                  {t.myTickets.completePayment}
                </Link>
              }
            />
          ))}
        </TicketSection>
      )}

      {upcoming.length > 0 && (
        <TicketSection title={t.myTickets.upcoming}>
          {upcoming.map((booking) => (
            <TicketStub
              key={booking.id}
              booking={booking}
              actions={booking.cancellable ? <CancelTicket booking={booking} onCancelled={replace} /> : null}
            />
          ))}
        </TicketSection>
      )}

      {history.length > 0 && (
        <TicketSection title={t.myTickets.history}>
          {history.map((booking) => (
            <TicketStub key={booking.id} booking={booking} />
          ))}
        </TicketSection>
      )}
    </div>
  )
}

function TicketSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="space-y-5">
      <h2 className="text-3xl font-bold">{title}</h2>
      <div className="space-y-5">{children}</div>
    </section>
  )
}

/** Two-step cancel so a stray click doesn't throw tickets away. */
function CancelTicket({ booking, onCancelled }: { booking: Booking; onCancelled: (booking: Booking) => void }) {
  const { t, errorText } = useI18n()
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function cancel() {
    setBusy(true)
    try {
      onCancelled(await api.bookings.cancel(booking.id))
    } catch (e) {
      setError(errorText(e, t.myTickets.cancelFailed))
      setBusy(false)
    }
  }

  if (!confirming) {
    return (
      <button type="button" className="btn btn-quiet" onClick={() => setConfirming(true)}>
        {t.myTickets.cancelTickets}
      </button>
    )
  }
  return (
    <div className="flex flex-wrap items-center gap-3 rounded-xl bg-paper px-4 py-3">
      <span className="text-sm">{t.myTickets.confirmCancel(booking.seats.length)}</span>
      <button type="button" className="btn btn-primary px-4 py-2 text-sm" onClick={cancel} disabled={busy}>
        {busy ? t.myTickets.cancelling : t.myTickets.yesCancel}
      </button>
      <button type="button" className="text-sm font-semibold underline-offset-4 hover:underline" onClick={() => setConfirming(false)}>
        {t.myTickets.keep}
      </button>
      {error && (
        <p role="alert" className="w-full text-sm text-curtain">
          {error}
        </p>
      )}
    </div>
  )
}
