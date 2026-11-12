import { useState, type FormEvent } from 'react'
import { api } from '../../api/endpoints'
import { ApiErrorNotice, PageLoader } from '../../components/Feedback'
import { useI18n } from '../../i18n/I18nContext'
import { useApi } from '../../lib/useApi'
import { Field } from '../AuthPages'

const empty = { name: '', city: '', address: '', rows: '10', seatsPerRow: '16' }

export function AdminVenuesPage() {
  const { t, errorText, fieldErrorsText } = useI18n()
  const venues = useApi('venues', () => api.admin.venues())
  const [form, setForm] = useState(empty)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const set = (key: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [key]: e.target.value }))
  const capacity = (Number(form.rows) || 0) * (Number(form.seatsPerRow) || 0)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setErrors({})
    setMessage(null)
    try {
      const venue = await api.admin.createVenue({
        name: form.name,
        city: form.city,
        address: form.address,
        rows: Number(form.rows),
        seatsPerRow: Number(form.seatsPerRow),
      })
      setForm(empty)
      setMessage(t.admin.venueAdded(venue.name))
      venues.reload()
    } catch (e) {
      const fields = fieldErrorsText(e)
      setErrors(fields)
      setMessage(Object.keys(fields).length === 0 ? errorText(e) : null)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid gap-8 lg:grid-cols-[1fr_22rem] lg:items-start">
      <section className="space-y-4">
        <h2 className="text-4xl font-bold">{t.admin.venues}</h2>
        {venues.error && <ApiErrorNotice error={venues.error} onRetry={venues.reload} />}
        {venues.loading && !venues.data ? (
          <PageLoader />
        ) : (
          <ul className="divide-y divide-line rounded-3xl border border-line bg-card">
            {(venues.data ?? []).map((venue) => (
              <li key={venue.id} className="flex flex-wrap items-baseline justify-between gap-2 px-5 py-4">
                <div>
                  <p className="font-display text-2xl font-bold">{venue.name}</p>
                  <p className="text-sm text-ink-soft">
                    {venue.address}, {venue.city}
                  </p>
                </div>
                <p className="text-sm">
                  {t.admin.venueSeats(venue.capacity)}{' '}
                  <span className="text-ink-soft">{t.admin.venueLayout(venue.rows, venue.seatsPerRow)}</span>
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>

      <form onSubmit={submit} noValidate className="space-y-4 rounded-3xl border border-line bg-card p-6">
        <h3 className="text-2xl font-bold">{t.admin.addVenueTitle}</h3>
        <Field label={t.admin.name} id="name" error={errors.name}>
          <input id="name" className="field" value={form.name} onChange={set('name')} aria-invalid={Boolean(errors.name)} />
        </Field>
        <Field label={t.admin.city} id="city" error={errors.city}>
          <input id="city" className="field" value={form.city} onChange={set('city')} aria-invalid={Boolean(errors.city)} />
        </Field>
        <Field label={t.admin.address} id="address" error={errors.address}>
          <input id="address" className="field" value={form.address} onChange={set('address')} aria-invalid={Boolean(errors.address)} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label={t.admin.rows} id="rows" error={errors.rows} hint={t.admin.range(1, 26)}>
            <input id="rows" type="number" min={1} max={26} className="field" value={form.rows} onChange={set('rows')} />
          </Field>
          <Field label={t.admin.seatsPerRow} id="seatsPerRow" error={errors.seatsPerRow} hint={t.admin.range(1, 40)}>
            <input id="seatsPerRow" type="number" min={1} max={40} className="field" value={form.seatsPerRow} onChange={set('seatsPerRow')} />
          </Field>
        </div>
        <p className="text-sm text-ink-soft">{t.admin.capacity(capacity)}</p>
        {message && (
          <p role="status" className="text-sm font-semibold">
            {message}
          </p>
        )}
        <button type="submit" className="btn btn-primary w-full" disabled={busy}>
          {busy ? t.admin.addingVenue : t.admin.addVenue}
        </button>
      </form>
    </div>
  )
}
