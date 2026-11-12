import { api } from '../../api/endpoints'
import { ApiErrorNotice, PageLoader } from '../../components/Feedback'
import { SalesChart } from '../../components/SalesChart'
import { useI18n } from '../../i18n/I18nContext'
import { useApi } from '../../lib/useApi'

export function AdminDashboardPage() {
  const { t, f } = useI18n()
  const dashboard = useApi('dashboard', () => api.admin.dashboard())

  if (dashboard.loading && !dashboard.data) return <PageLoader />
  if (dashboard.error) return <ApiErrorNotice error={dashboard.error} onRetry={dashboard.reload} />
  if (!dashboard.data) return null
  const d = dashboard.data

  const stats = [
    { key: 'revenue', label: t.admin.stats.revenue, value: f.money(d.totalRevenue) },
    { key: 'tickets', label: t.admin.stats.ticketsSold, value: f.number(d.ticketsSold) },
    { key: 'bookings', label: t.admin.stats.paidBookings, value: f.number(d.confirmedBookings) },
    { key: 'events', label: t.admin.stats.upcomingEvents, value: f.number(d.upcomingEvents) },
    { key: 'users', label: t.admin.stats.users, value: f.number(d.registeredUsers) },
  ]

  return (
    <div className="space-y-8">
      <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-3xl border border-line bg-line sm:grid-cols-3 lg:grid-cols-5">
        {stats.map((stat) => (
          <div key={stat.key} className="bg-card px-5 py-4">
            <dt className="text-sm text-ink-soft">{stat.label}</dt>
            <dd className="mt-1 font-display text-4xl font-bold">{stat.value}</dd>
          </div>
        ))}
      </dl>

      <SalesChart days={d.salesLast14Days} />

      <section className="rounded-3xl border border-line bg-card p-6">
        <h2 className="text-3xl font-bold">{t.admin.latestBookings}</h2>
        <div className="mt-4 overflow-x-auto">
          <table className="w-full min-w-[36rem] text-sm">
            <thead className="text-left text-ink-soft">
              <tr>
                <th className="py-2 font-semibold">{t.admin.reference}</th>
                <th className="py-2 font-semibold">{t.admin.customer}</th>
                <th className="py-2 font-semibold">{t.admin.event}</th>
                <th className="py-2 text-right font-semibold">{t.admin.tickets}</th>
                <th className="py-2 text-right font-semibold">{t.admin.amount}</th>
                <th className="py-2 text-right font-semibold">{t.admin.paidAt}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {d.recentBookings.map((b) => (
                <tr key={b.reference}>
                  <td className="py-2.5 font-semibold">{b.reference}</td>
                  <td className="py-2.5">{b.customerName}</td>
                  <td className="py-2.5">{b.eventTitle}</td>
                  <td className="py-2.5 text-right tabular-nums">{b.tickets}</td>
                  <td className="py-2.5 text-right tabular-nums">{f.money(b.amount)}</td>
                  <td className="py-2.5 text-right text-ink-soft">{f.shortDate(b.confirmedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
