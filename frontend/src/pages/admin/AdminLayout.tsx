import { NavLink, Outlet } from 'react-router'
import { useI18n } from '../../i18n/I18nContext'

export function AdminLayout() {
  const { t } = useI18n()
  const tabs = [
    { to: '/admin', label: t.admin.dashboard, end: true },
    { to: '/admin/events', label: t.admin.events, end: false },
    { to: '/admin/venues', label: t.admin.venues, end: false },
  ]

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b border-line pb-4">
        <h1 className="text-6xl font-extrabold">{t.admin.title}</h1>
        <nav aria-label={t.admin.title} className="flex gap-1">
          {tabs.map((tab) => (
            <NavLink
              key={tab.to}
              to={tab.to}
              end={tab.end}
              className={({ isActive }) =>
                `rounded-full px-4 py-2 text-sm font-semibold ${isActive ? 'bg-curtain text-white' : 'hover:bg-ink/8'}`
              }
            >
              {tab.label}
            </NavLink>
          ))}
        </nav>
      </div>
      <Outlet />
    </div>
  )
}
