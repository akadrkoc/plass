import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { api } from '../../api/endpoints'
import type { EventCategory } from '../../api/types'
import { ApiErrorNotice, ErrorNotice, PageLoader } from '../../components/Feedback'
import { useI18n } from '../../i18n/I18nContext'
import { useApi } from '../../lib/useApi'
import { Field } from '../AuthPages'

const CATEGORIES: EventCategory[] = ['CONCERT', 'THEATRE', 'COMEDY', 'CLASSICAL', 'FESTIVAL', 'KIDS']

export function AdminNewEventPage() {
  const { t, errorText, fieldErrorsText } = useI18n()
  const navigate = useNavigate()
  const venues = useApi('venues', () => api.admin.venues())
  const [form, setForm] = useState({
    title: '',
    description: '',
    category: 'CONCERT' as EventCategory,
    venueId: '',
    startsAt: '',
    vipPrice: '',
    premiumPrice: '',
    standardPrice: '',
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const set = (key: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [key]: e.target.value }))

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setErrors({})
    setError(null)
    try {
      await api.admin.createEvent({
        title: form.title,
        description: form.description,
        category: form.category,
        venueId: Number(form.venueId),
        // datetime-local has no zone: interpret it as the organiser's local time.
        startsAt: form.startsAt ? new Date(form.startsAt).toISOString() : '',
        vipPrice: Number(form.vipPrice),
        premiumPrice: Number(form.premiumPrice),
        standardPrice: Number(form.standardPrice),
      })
      navigate('/admin/events')
    } catch (e) {
      const fields = fieldErrorsText(e)
      setErrors(fields)
      setError(Object.keys(fields).length > 0 ? t.admin.fixFields : errorText(e, t.admin.createFailed))
      setBusy(false)
    }
  }

  if (venues.loading && !venues.data) return <PageLoader />
  if (venues.error) return <ApiErrorNotice error={venues.error} onRetry={venues.reload} />
  const venueList = venues.data ?? []
  const venue = venueList.find((v) => String(v.id) === form.venueId)

  const priceFields = [
    ['vipPrice', t.tier.VIP],
    ['premiumPrice', t.tier.PREMIUM],
    ['standardPrice', t.tier.STANDARD],
  ] as const

  return (
    <section className="max-w-3xl space-y-6">
      <div>
        <Link to="/admin/events" className="text-sm font-semibold text-ink-soft hover:underline">
          {t.admin.events}
        </Link>
        <h2 className="mt-1 text-4xl font-bold">{t.admin.createEvent}</h2>
        <p className="text-ink-soft">{t.admin.newEventIntro}</p>
      </div>

      {venueList.length === 0 ? (
        <ErrorNotice message={t.admin.noVenues} />
      ) : (
        <form onSubmit={submit} noValidate className="space-y-5 rounded-3xl border border-line bg-card p-6 sm:p-8">
          <Field label={t.admin.eventTitle} id="title" error={errors.title}>
            <input id="title" className="field" value={form.title} onChange={set('title')} aria-invalid={Boolean(errors.title)} />
          </Field>
          <Field label={t.admin.description} id="description" error={errors.description}>
            <textarea
              id="description"
              rows={4}
              className="field"
              value={form.description}
              onChange={set('description')}
              aria-invalid={Boolean(errors.description)}
            />
          </Field>
          <div className="grid gap-5 sm:grid-cols-2">
            <Field label={t.admin.category} id="category" error={errors.category}>
              <select id="category" className="field" value={form.category} onChange={set('category')}>
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {t.category[c]}
                  </option>
                ))}
              </select>
            </Field>
            <Field label={t.admin.dateTime} id="startsAt" error={errors.startsAt}>
              <input
                id="startsAt"
                type="datetime-local"
                className="field"
                value={form.startsAt}
                onChange={set('startsAt')}
                aria-invalid={Boolean(errors.startsAt)}
              />
            </Field>
          </div>
          <Field
            label={t.admin.venue}
            id="venueId"
            error={errors.venueId}
            hint={venue ? t.admin.venueHint(venue.capacity, venue.rows, venue.seatsPerRow) : undefined}
          >
            <select id="venueId" className="field" value={form.venueId} onChange={set('venueId')} aria-invalid={Boolean(errors.venueId)}>
              <option value="">{t.admin.chooseVenue}</option>
              {venueList.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.name}, {v.city}
                </option>
              ))}
            </select>
          </Field>
          <fieldset>
            <legend className="label">{t.admin.prices}</legend>
            <div className="grid gap-4 sm:grid-cols-3">
              {priceFields.map(([key, label]) => (
                <Field key={key} label={label} id={key} error={errors[key]}>
                  <input
                    id={key}
                    type="number"
                    min={1}
                    step={1}
                    inputMode="numeric"
                    className="field"
                    value={form[key]}
                    onChange={set(key)}
                    aria-invalid={Boolean(errors[key])}
                  />
                </Field>
              ))}
            </div>
          </fieldset>
          {error && (
            <p role="alert" className="font-semibold text-curtain">
              {error}
            </p>
          )}
          <div className="flex gap-3">
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? t.admin.creating : t.admin.createEvent}
            </button>
            <Link to="/admin/events" className="btn btn-quiet">
              {t.admin.cancel}
            </Link>
          </div>
        </form>
      )}
    </section>
  )
}
