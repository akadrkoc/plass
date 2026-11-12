import { useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { useI18n } from '../i18n/I18nContext'

const DEMO_ACCOUNTS = [
  { role: 'buyer', email: 'demo@plass.dev', password: 'Demo1234!' },
  { role: 'organiser', email: 'admin@plass.dev', password: 'Admin123!' },
] as const

/** Only follow same-site paths after login (avoids open redirects). */
function useNextPath() {
  const [params] = useSearchParams()
  const next = params.get('next')
  return next && next.startsWith('/') && !next.startsWith('//') ? next : '/'
}

export function LoginPage() {
  const { user, login } = useAuth()
  const { t, errorText } = useI18n()
  const navigate = useNavigate()
  const next = useNextPath()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={next} replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const loggedIn = await login(email, password)
      navigate(loggedIn.role === 'ADMIN' && next === '/' ? '/admin' : next, { replace: true })
    } catch (e) {
      setError(errorText(e, t.auth.loginFailed))
      setBusy(false)
    }
  }

  return (
    <AuthFrame title={t.auth.loginTitle} intro={t.auth.loginIntro}>
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label={t.auth.email} id="email">
          <input id="email" type="email" autoComplete="email" required className="field" value={email} onChange={(e) => setEmail(e.target.value)} />
        </Field>
        <Field label={t.auth.password} id="password">
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            className="field"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>
        {error && (
          <p role="alert" className="text-sm font-semibold text-curtain">
            {error}
          </p>
        )}
        <button type="submit" className="btn btn-primary w-full" disabled={busy}>
          {busy ? t.auth.loggingIn : t.auth.logIn}
        </button>
      </form>

      <div className="mt-8 rounded-2xl border border-line bg-paper p-4">
        <p className="font-semibold">{t.auth.demoTitle}</p>
        <p className="text-sm text-ink-soft">{t.auth.demoBody}</p>
        <ul className="mt-3 space-y-2">
          {DEMO_ACCOUNTS.map((account) => (
            <li key={account.email} className="flex items-center justify-between gap-3 text-sm">
              <span>
                <span className="font-semibold">{account.role === 'buyer' ? t.auth.demoBuyer : t.auth.demoOrganiser}</span>
                <span className="block text-ink-soft">
                  {account.email} / {account.password}
                </span>
              </span>
              <button
                type="button"
                className="btn btn-quiet px-3 py-1.5 text-sm"
                onClick={() => {
                  setEmail(account.email)
                  setPassword(account.password)
                }}
              >
                {t.auth.fillIn}
              </button>
            </li>
          ))}
        </ul>
      </div>

      <p className="mt-6 text-center text-sm">
        {t.auth.newHere}{' '}
        <Link to={`/register${next !== '/' ? `?next=${encodeURIComponent(next)}` : ''}`} className="font-semibold text-curtain hover:underline">
          {t.auth.createAccountLink}
        </Link>
      </p>
    </AuthFrame>
  )
}

export function RegisterPage() {
  const { user, register } = useAuth()
  const { t, errorText, fieldErrorsText } = useI18n()
  const navigate = useNavigate()
  const next = useNextPath()
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={next} replace />

  const set = (key: keyof typeof form) => (e: ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }))

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setErrors({})
    try {
      await register(form.fullName, form.email, form.password)
      navigate(next, { replace: true })
    } catch (e) {
      const fields = fieldErrorsText(e)
      setErrors(fields)
      setError(Object.keys(fields).length > 0 ? null : errorText(e, t.auth.registerFailed))
      setBusy(false)
    }
  }

  return (
    <AuthFrame title={t.auth.registerTitle} intro={t.auth.registerIntro}>
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label={t.auth.fullName} id="fullName" error={errors.fullName}>
          <input id="fullName" autoComplete="name" className="field" value={form.fullName} onChange={set('fullName')} aria-invalid={Boolean(errors.fullName)} />
        </Field>
        <Field label={t.auth.email} id="email" error={errors.email}>
          <input id="email" type="email" autoComplete="email" className="field" value={form.email} onChange={set('email')} aria-invalid={Boolean(errors.email)} />
        </Field>
        <Field label={t.auth.password} id="password" error={errors.password} hint={t.auth.passwordHint}>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            className="field"
            value={form.password}
            onChange={set('password')}
            aria-invalid={Boolean(errors.password)}
          />
        </Field>
        {error && (
          <p role="alert" className="text-sm font-semibold text-curtain">
            {error}
          </p>
        )}
        <button type="submit" className="btn btn-primary w-full" disabled={busy}>
          {busy ? t.auth.creating : t.auth.createAccount}
        </button>
      </form>
      <p className="mt-6 text-center text-sm">
        {t.auth.haveAccount}{' '}
        <Link to={`/login${next !== '/' ? `?next=${encodeURIComponent(next)}` : ''}`} className="font-semibold text-curtain hover:underline">
          {t.auth.logIn}
        </Link>
      </p>
    </AuthFrame>
  )
}

function AuthFrame({ title, intro, children }: { title: string; intro: string; children: ReactNode }) {
  return (
    <div className="mx-auto max-w-md py-6">
      <h1 className="text-6xl font-extrabold">{title}</h1>
      <p className="mt-2 text-ink-soft">{intro}</p>
      <div className="mt-8 rounded-3xl border border-line bg-card p-6 sm:p-8">{children}</div>
    </div>
  )
}

export function Field({
  label,
  id,
  error,
  hint,
  children,
}: {
  label: string
  id: string
  error?: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div>
      <label htmlFor={id} className="label">
        {label}
      </label>
      {children}
      {error ? (
        <p className="mt-1 text-sm font-semibold text-curtain">{capitalize(error)}</p>
      ) : (
        hint && <p className="mt-1 text-sm text-ink-soft">{hint}</p>
      )}
    </div>
  )
}

const capitalize = (text: string) => text.charAt(0).toUpperCase() + text.slice(1)
