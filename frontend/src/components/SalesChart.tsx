import { useState } from 'react'
import type { Dashboard } from '../api/types'
import { useI18n } from '../i18n/I18nContext'

type Day = Dashboard['salesLast14Days'][number]

/** Daily ticket revenue as columns. Hover or focus a day for details; a table view is one click away. */
export function SalesChart({ days }: { days: Day[] }) {
  const { t, f } = useI18n()
  const [active, setActive] = useState<number | null>(null)
  const [asTable, setAsTable] = useState(false)
  const max = niceMax(Math.max(...days.map((d) => d.revenue), 1))
  const peak = days.reduce((best, day, i) => (day.revenue > days[best].revenue ? i : best), 0)
  const shown = active ?? null

  return (
    <figure className="rounded-3xl border border-line bg-card p-6">
      <div className="flex flex-wrap items-baseline justify-between gap-3">
        <figcaption>
          <h2 className="text-3xl font-bold">{t.admin.chart.title}</h2>
          <p className="text-sm text-ink-soft">{t.admin.chart.subtitle}</p>
        </figcaption>
        <button type="button" className="text-sm font-semibold underline underline-offset-4" onClick={() => setAsTable((v) => !v)}>
          {asTable ? t.admin.chart.showChart : t.admin.chart.showTable}
        </button>
      </div>

      {asTable ? (
        <table className="mt-5 w-full text-sm">
          <thead className="text-left text-ink-soft">
            <tr>
              <th className="py-2 font-semibold">{t.admin.chart.day}</th>
              <th className="py-2 text-right font-semibold">{t.admin.chart.tickets}</th>
              <th className="py-2 text-right font-semibold">{t.admin.chart.revenue}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {days.map((day) => (
              <tr key={day.date}>
                <td className="py-2">{f.day(day.date, { weekday: 'short', day: 'numeric', month: 'short' })}</td>
                <td className="py-2 text-right tabular-nums">{day.tickets}</td>
                <td className="py-2 text-right tabular-nums">{f.money(day.revenue)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <div className="mt-6 grid grid-cols-[4rem_1fr] gap-2">
          {/* y axis */}
          <div className="relative h-56 text-right text-xs text-ink-soft" aria-hidden>
            {[1, 0.5, 0].map((share) => (
              <span key={share} className="absolute right-0 -translate-y-1/2" style={{ top: `${(1 - share) * 100}%` }}>
                {f.money(max * share)}
              </span>
            ))}
          </div>
          <div className="relative">
            {[1, 0.5].map((share) => (
              <div key={share} className="absolute inset-x-0 border-t border-dashed border-line" style={{ top: `${(1 - share) * 100}%` }} aria-hidden />
            ))}
            <ol className="relative flex h-56 items-end border-b border-ink-soft/50" onMouseLeave={() => setActive(null)}>
              {days.map((day, i) => {
                const height = (day.revenue / max) * 100
                const description = `${f.day(day.date, { weekday: 'long', day: 'numeric', month: 'long' })}: ${f.money(
                  day.revenue,
                )}, ${t.common.ticketCount(day.tickets)}`
                return (
                  <li key={day.date} className="relative flex h-full flex-1 items-end justify-center">
                    <button
                      type="button"
                      className="absolute inset-0 cursor-default"
                      aria-label={description}
                      onMouseEnter={() => setActive(i)}
                      onFocus={() => setActive(i)}
                      onBlur={() => setActive(null)}
                    />
                    {i === peak && day.revenue > 0 && shown === null && (
                      <span className="pointer-events-none absolute -translate-y-full pb-1 text-xs font-semibold" style={{ bottom: `${height}%` }}>
                        {f.money(day.revenue)}
                      </span>
                    )}
                    <span
                      className={`pointer-events-none w-full max-w-6 rounded-t-[4px] transition-opacity ${
                        shown !== null && shown !== i ? 'opacity-40' : ''
                      }`}
                      style={{ height: `${height}%`, background: 'var(--color-chart-sales)', marginInline: 1 }}
                    />
                    {shown === i && (
                      <span
                        role="tooltip"
                        className="pointer-events-none absolute z-10 w-max -translate-y-full rounded-lg bg-ink px-3 py-2 text-xs text-white shadow-lg"
                        style={{ bottom: `calc(${height}% + 8px)` }}
                      >
                        <span className="block font-semibold">{f.day(day.date, { weekday: 'short', day: 'numeric', month: 'short' })}</span>
                        {f.money(day.revenue)}, {t.common.ticketCount(day.tickets)}
                      </span>
                    )}
                  </li>
                )
              })}
            </ol>
            <ol className="mt-1.5 flex text-center text-xs text-ink-soft" aria-hidden>
              {days.map((day, i) => (
                <li key={day.date} className="flex-1">
                  {i % 2 === 0 || i === days.length - 1 ? f.day(day.date, { day: 'numeric', month: i === 0 ? 'short' : undefined }) : ''}
                </li>
              ))}
            </ol>
          </div>
        </div>
      )}
    </figure>
  )
}

/** Rounds the axis maximum up to a friendly number (1, 2 or 5 × 10^n). */
function niceMax(value: number) {
  const magnitude = 10 ** Math.floor(Math.log10(value))
  const step = [1, 2, 5, 10].find((s) => s * magnitude >= value) ?? 10
  return step * magnitude
}
