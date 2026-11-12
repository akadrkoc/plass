import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api } from '../api/endpoints'
import type { EventCategory, EventSummary } from '../api/types'
import { ApiErrorNotice, EmptyState, PageLoader } from '../components/Feedback'
import { EventRow } from '../components/EventRow'
import { useI18n } from '../i18n/I18nContext'
import { useApi } from '../lib/useApi'

const PAGE_SIZE = 8

export function EventsPage() {
  const { t } = useI18n()
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const city = params.get('city') ?? ''
  const category = (params.get('category') as EventCategory | null) ?? undefined

  const filters = useApi('filters', () => api.events.filters())

  function update(key: string, value: string | undefined) {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    setParams(next, { replace: true })
  }

  return (
    <div className="space-y-10">
      <ComingUp />

      <section aria-labelledby="programme-title" className="space-y-6">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 id="programme-title" className="text-5xl font-extrabold">
              {t.events.title}
            </h1>
            <p className="mt-1 text-ink-soft">{t.events.intro}</p>
          </div>
          <SearchForm key={q} initial={q} onSearch={(text) => update('q', text || undefined)} />
        </div>

        <div className="flex flex-wrap gap-x-8 gap-y-3">
          <FilterGroup
            label={t.events.city}
            options={(filters.data?.cities ?? []).map((value) => ({ value, label: value }))}
            value={city}
            onChange={(value) => update('city', value)}
            allLabel={t.events.allCities}
          />
          <FilterGroup
            label={t.events.category}
            options={(filters.data?.categories ?? []).map((value) => ({ value, label: t.category[value] }))}
            value={category ?? ''}
            onChange={(value) => update('category', value)}
            allLabel={t.events.everything}
          />
        </div>

        {/* Keyed by the filters so paging state starts fresh whenever they change. */}
        <Programme
          key={`${q}|${city}|${category ?? ''}`}
          q={q}
          city={city}
          category={category}
          onClear={() => setParams({}, { replace: true })}
        />
      </section>
    </div>
  )
}

/** The marquee: the next three shows in lights. */
function ComingUp() {
  const { t, f } = useI18n()
  const highlights = useApi('highlights', () => api.events.search({ size: 3 }))
  if (highlights.data?.content.length === 0) return null

  return (
    <section className="marquee" aria-labelledby="marquee-title">
      <h2 id="marquee-title" className="mb-4 font-sans text-base font-normal text-brass-light">
        {t.events.comingUp}
      </h2>
      {highlights.loading ? (
        <div className="h-40" />
      ) : (
        <ol className="space-y-3">
          {highlights.data?.content.map((event) => {
            const { day, month, weekday, time } = f.dateParts(event.startsAt)
            return (
              <li key={event.id}>
                <Link to={`/events/${event.id}`} className="group grid gap-x-6 sm:grid-cols-[11rem_1fr_auto] sm:items-baseline">
                  <span className="font-display text-xl font-semibold text-brass-light sm:text-2xl">
                    {weekday} {day} {month}, {time}
                  </span>
                  <span className="font-display text-4xl font-extrabold uppercase leading-none tracking-wide group-hover:text-brass-light sm:text-5xl">
                    {event.title}
                  </span>
                  <span className="text-white/70">{event.city}</span>
                </Link>
              </li>
            )
          })}
        </ol>
      )}
    </section>
  )
}

function SearchForm({ initial, onSearch }: { initial: string; onSearch: (text: string) => void }) {
  const { t } = useI18n()
  const [text, setText] = useState(initial)
  function submit(event: FormEvent) {
    event.preventDefault()
    onSearch(text.trim())
  }
  return (
    <form onSubmit={submit} role="search" className="flex w-full gap-2 sm:w-auto">
      <label htmlFor="search" className="sr-only">
        {t.events.searchLabel}
      </label>
      <input
        id="search"
        type="search"
        className="field sm:w-72"
        placeholder={t.events.searchLabel}
        value={text}
        onChange={(e) => setText(e.target.value)}
      />
      <button type="submit" className="btn btn-primary">
        {t.events.search}
      </button>
    </form>
  )
}

interface ProgrammeProps {
  q: string
  city: string
  category?: EventCategory
  onClear: () => void
}

function Programme({ q, city, category, onClear }: ProgrammeProps) {
  const { t } = useI18n()
  const results = useApi(`events:${q}|${city}|${category}`, () => api.events.search({ q, city, category, size: PAGE_SIZE }))
  const [extra, setExtra] = useState<EventSummary[]>([])
  const [nextPage, setNextPage] = useState(1)
  const [loadingMore, setLoadingMore] = useState(false)
  const filtersActive = Boolean(q || city || category)

  async function loadMore() {
    setLoadingMore(true)
    try {
      const page = await api.events.search({ q, city, category, size: PAGE_SIZE, page: nextPage })
      setExtra((items) => [...items, ...page.content])
      setNextPage((n) => n + 1)
    } finally {
      setLoadingMore(false)
    }
  }

  if (results.error) return <ApiErrorNotice error={results.error} onRetry={results.reload} />
  if (results.loading && !results.data) return <PageLoader label={t.events.loadingEvents} />

  const events = [...(results.data?.content ?? []), ...extra]
  const total = results.data?.totalElements ?? 0

  if (events.length === 0) {
    return (
      <EmptyState title={t.events.noMatchTitle}>
        <p>{t.events.noMatchBody}</p>
        {filtersActive && (
          <button type="button" className="btn btn-quiet mt-5" onClick={onClear}>
            {t.events.clearFilters}
          </button>
        )}
      </EmptyState>
    )
  }

  return (
    <>
      <p className="text-sm text-ink-soft" aria-live="polite">
        {t.events.count(total, filtersActive)}
      </p>
      <ul className="divide-y divide-line border-y border-line">
        {events.map((event) => (
          <EventRow key={event.id} event={event} />
        ))}
      </ul>
      {events.length < total && (
        <div className="text-center">
          <button type="button" className="btn btn-quiet" onClick={loadMore} disabled={loadingMore}>
            {loadingMore ? t.common.loading : t.events.showMore}
          </button>
        </div>
      )}
    </>
  )
}

interface FilterGroupProps {
  label: string
  options: { value: string; label: string }[]
  value: string
  onChange: (value: string | undefined) => void
  allLabel: string
}

function FilterGroup({ label, options, value, onChange, allLabel }: FilterGroupProps) {
  const chip = (active: boolean) =>
    `rounded-full border-[1.5px] px-3.5 py-1.5 text-sm font-semibold transition-colors ${
      active ? 'border-ink bg-ink text-white' : 'border-line bg-card hover:border-ink-soft'
    }`
  return (
    <fieldset className="flex flex-wrap items-center gap-2">
      <legend className="sr-only">{label}</legend>
      <button type="button" className={chip(!value)} aria-pressed={!value} onClick={() => onChange(undefined)}>
        {allLabel}
      </button>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={chip(value === option.value)}
          aria-pressed={value === option.value}
          onClick={() => onChange(value === option.value ? undefined : option.value)}
        >
          {option.label}
        </button>
      ))}
    </fieldset>
  )
}
