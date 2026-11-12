import { useState } from 'react'
import { Link } from 'react-router'
import { api } from '../../api/endpoints'
import type { AdminEvent } from '../../api/types'
import { ApiErrorNotice, EmptyState, PageLoader } from '../../components/Feedback'
import { useI18n } from '../../i18n/I18nContext'
import { useApi, useNow } from '../../lib/useApi'

export function AdminEventsPage() {
  const { t } = useI18n()
  const events = useApi('admin-events', () => api.admin.events())

  if (events.loading && !events.data) return <PageLoader />
  if (events.error) return <ApiErrorNotice error={events.error} onRetry={events.reload} />
  const page = events.data
  if (!page) return null

  function replace(updated: AdminEvent) {
    if (!page) return
    events.setData({ ...page, content: page.content.map((e) => (e.id === updated.id ? updated : e)) })
  }

  return (
    <section className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-4xl font-bold">{t.admin.events}</h2>
          <p className="text-ink-soft">{t.admin.eventsIntro}</p>
        </div>
        <Link to="/admin/events/new" className="btn btn-primary">
          {t.admin.createEvent}
        </Link>
      </div>

      {page.content.length === 0 ? (
        <EmptyState title={t.admin.noEventsTitle}>
          <p>{t.admin.noEventsBody}</p>
        </EmptyState>
      ) : (
        <div className="overflow-x-auto rounded-3xl border border-line bg-card">
          <table className="w-full min-w-[52rem] text-sm">
            <caption className="px-5 pt-4 text-left">
              <OccupancyLegend />
            </caption>
            <thead className="text-left text-ink-soft">
              <tr>
                <th className="px-5 py-3 font-semibold">{t.admin.date}</th>
                <th className="py-3 font-semibold">{t.admin.event}</th>
                <th className="py-3 font-semibold">{t.admin.occupancy}</th>
                <th className="py-3 text-right font-semibold">{t.admin.revenue}</th>
                <th className="px-5 py-3 text-right font-semibold">
                  <span className="sr-only">{t.admin.actions}</span>
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {page.content.map((event) => (
                <EventLine key={event.id} event={event} onChange={replace} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function EventLine({ event, onChange }: { event: AdminEvent; onChange: (event: AdminEvent) => void }) {
  const { t, f, errorText } = useI18n()
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const now = useNow()
  const past = new Date(event.startsAt).getTime() < now
  const cancelled = event.status === 'CANCELLED'

  async function cancel() {
    setBusy(true)
    try {
      onChange(await api.admin.cancelEvent(event.id))
      setConfirming(false)
    } catch (e) {
      setError(errorText(e, t.admin.cancelFailed))
    } finally {
      setBusy(false)
    }
  }

  return (
    <tr className={cancelled || past ? 'text-ink-soft' : undefined}>
      <td className="whitespace-nowrap px-5 py-3 align-top">{f.shortDate(event.startsAt)}</td>
      <td className="py-3 pr-4 align-top">
        <Link to={`/events/${event.id}`} className="font-semibold text-ink hover:underline">
          {event.title}
        </Link>
        <span className="block">
          {t.category[event.category]}, {event.venueName}, {event.city}
        </span>
        {(cancelled || past) && (
          <span className="mt-1 inline-block rounded-full bg-paper px-2 py-0.5 text-xs font-semibold">
            {cancelled ? t.admin.statusCancelled : t.admin.statusPast}
          </span>
        )}
      </td>
      <td className="w-60 py-3 pr-4 align-top">
        <Occupancy event={event} />
      </td>
      <td className="py-3 text-right align-top tabular-nums">{f.money(event.revenue)}</td>
      <td className="px-5 py-3 text-right align-top">
        {!cancelled && !past &&
          (confirming ? (
            <span className="flex flex-col items-end gap-1.5">
              <span className="text-xs">{t.admin.refundWarning}</span>
              <span className="flex gap-2">
                <button type="button" className="btn btn-primary px-3 py-1.5 text-xs" onClick={cancel} disabled={busy}>
                  {busy ? t.admin.cancelling : t.admin.cancelEvent}
                </button>
                <button type="button" className="text-xs font-semibold hover:underline" onClick={() => setConfirming(false)}>
                  {t.admin.keep}
                </button>
              </span>
              {error && <span className="text-xs text-curtain">{error}</span>}
            </span>
          ) : (
            <button type="button" className="text-sm font-semibold text-curtain hover:underline" onClick={() => setConfirming(true)}>
              {t.admin.cancelEvent}
            </button>
          ))}
      </td>
    </tr>
  )
}

function Occupancy({ event }: { event: AdminEvent }) {
  const { t } = useI18n()
  const pct = (n: number) => (event.totalSeats === 0 ? 0 : (n / event.totalSeats) * 100)
  const soldPct = Math.round(pct(event.bookedSeats))
  return (
    <div title={t.admin.occupancyTitle(event.bookedSeats, event.heldSeats, event.availableSeats)}>
      <div className="flex h-2.5 gap-[2px] overflow-hidden rounded-full bg-line" aria-hidden>
        {event.bookedSeats > 0 && <span className="h-full" style={{ width: `${pct(event.bookedSeats)}%`, background: 'var(--color-chart-sold)' }} />}
        {event.heldSeats > 0 && <span className="h-full" style={{ width: `${pct(event.heldSeats)}%`, background: 'var(--color-chart-held)' }} />}
      </div>
      <p className="mt-1 text-xs">
        {t.admin.occupancyText(soldPct, event.bookedSeats, event.totalSeats)}
        {event.heldSeats > 0 && t.admin.heldSuffix(event.heldSeats)}
      </p>
    </div>
  )
}

function OccupancyLegend() {
  const { t } = useI18n()
  return (
    <span className="flex gap-4 text-xs text-ink-soft">
      <span className="flex items-center gap-1.5">
        <span className="size-2.5 rounded-sm" style={{ background: 'var(--color-chart-sold)' }} aria-hidden />
        {t.admin.sold}
      </span>
      <span className="flex items-center gap-1.5">
        <span className="size-2.5 rounded-sm" style={{ background: 'var(--color-chart-held)' }} aria-hidden />
        {t.admin.beingBooked}
      </span>
    </span>
  )
}
