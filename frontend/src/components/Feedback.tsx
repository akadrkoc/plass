import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { useI18n } from '../i18n/I18nContext'

export function PageLoader({ label }: { label?: string }) {
  const { t } = useI18n()
  return (
    <div className="flex min-h-[40vh] items-center justify-center" role="status">
      <span className="inline-flex items-center gap-3 text-ink-soft">
        <span className="size-5 animate-spin rounded-full border-2 border-line border-t-curtain" aria-hidden />
        {label ?? t.common.loading}
      </span>
    </div>
  )
}

export function ErrorNotice({ message, onRetry }: { message: string; onRetry?: () => void }) {
  const { t } = useI18n()
  return (
    <div role="alert" className="rounded-xl border border-curtain/30 bg-curtain/5 px-4 py-3 text-curtain-dark">
      <p>{message}</p>
      {onRetry && (
        <button type="button" onClick={onRetry} className="mt-2 font-semibold underline underline-offset-4">
          {t.common.tryAgain}
        </button>
      )}
    </div>
  )
}

/** ErrorNotice for an error thrown by the API client, translated into the current language. */
export function ApiErrorNotice({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const { errorText } = useI18n()
  return <ErrorNotice message={errorText(error)} onRetry={onRetry} />
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="rounded-2xl border-2 border-dashed border-line px-6 py-12 text-center">
      <h2 className="text-3xl font-bold">{title}</h2>
      {children && <div className="mx-auto mt-3 max-w-md text-ink-soft">{children}</div>}
    </div>
  )
}

export function NotFoundPage() {
  const { t } = useI18n()
  return (
    <div className="mx-auto max-w-xl py-24 text-center">
      <p className="font-display text-8xl font-extrabold text-curtain">404</p>
      <h1 className="mt-2 text-4xl font-bold">{t.notFound.title}</h1>
      <p className="mt-3 text-ink-soft">{t.notFound.body}</p>
      <Link to="/" className="btn btn-primary mt-8">
        {t.notFound.cta}
      </Link>
    </div>
  )
}
