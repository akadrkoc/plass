import { Link, NavLink, Outlet, ScrollRestoration } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { LANGUAGES, useI18n } from '../i18n/I18nContext'

const navClass = ({ isActive }: { isActive: boolean }) =>
  `whitespace-nowrap rounded-full px-3 py-1.5 font-semibold transition-colors ${
    isActive ? 'bg-ink text-white' : 'text-ink hover:bg-ink/8'
  }`

export function Layout() {
  const { user, logout } = useAuth()
  const { t } = useI18n()

  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 btn btn-primary">
        {t.layout.skipToContent}
      </a>
      <header className="border-b border-line bg-paper/90 backdrop-blur supports-[backdrop-filter]:sticky supports-[backdrop-filter]:top-0 supports-[backdrop-filter]:z-30">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-6 gap-y-2 px-5 py-3">
          <Link to="/" className="flex items-baseline gap-2" aria-label={t.layout.home}>
            <span className="font-display text-3xl font-extrabold tracking-wide text-curtain">Plass</span>
            <span className="hidden text-sm text-ink-soft sm:inline">{t.layout.tagline}</span>
          </Link>
          <nav className="flex flex-1 items-center gap-1 text-sm" aria-label={t.layout.mainNav}>
            <NavLink to="/" end className={navClass}>
              {t.layout.whatsOn}
            </NavLink>
            {user && (
              <NavLink to="/tickets" className={navClass}>
                {t.layout.myTickets}
              </NavLink>
            )}
            {user?.role === 'ADMIN' && (
              <NavLink to="/admin" className={navClass}>
                {t.layout.organiser}
              </NavLink>
            )}
          </nav>
          <div className="flex items-center gap-3 text-sm">
            <LanguageSwitch />
            {user ? (
              <>
                <span className="hidden text-ink-soft md:inline">{user.fullName}</span>
                <button type="button" onClick={logout} className="btn btn-quiet px-4 py-2">
                  {t.layout.logOut}
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="font-semibold hover:underline">
                  {t.layout.logIn}
                </Link>
                <Link to="/register" className="btn btn-primary px-4 py-2">
                  {t.layout.signUp}
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main id="main" className="mx-auto w-full max-w-6xl flex-1 px-5 py-8 sm:py-10">
        <Outlet />
      </main>

      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-6xl flex-wrap justify-between gap-2 px-5 py-6 text-sm text-ink-soft">
          <p>{t.layout.footer}</p>
          <a href="/swagger-ui.html" className="font-semibold text-ink hover:underline">
            {t.layout.apiDocs}
          </a>
        </div>
      </footer>
      <ScrollRestoration />
    </div>
  )
}

/** EN / NO toggle. Each button is labelled in its own language so people can find theirs. */
function LanguageSwitch() {
  const { language, setLanguage, t } = useI18n()
  return (
    <div role="group" aria-label={t.language.label} className="flex rounded-full border border-line p-0.5">
      {LANGUAGES.map((option) => (
        <button
          key={option}
          type="button"
          lang={option}
          aria-pressed={language === option}
          aria-label={t.language[option]}
          title={t.language[option]}
          onClick={() => setLanguage(option)}
          className={`rounded-full px-2.5 py-1 text-xs font-bold uppercase transition-colors ${
            language === option ? 'bg-ink text-white' : 'text-ink-soft hover:text-ink'
          }`}
        >
          {option === 'nb' ? 'no' : option}
        </button>
      ))}
    </div>
  )
}
