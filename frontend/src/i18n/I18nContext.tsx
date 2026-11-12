import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError } from '../api/client'
import { createFormatters, type Formatters } from './format'
import { messages, type Messages } from './messages'

export type Language = keyof typeof messages
export const LANGUAGES: Language[] = ['en', 'nb']

const LOCALES: Record<Language, string> = { en: 'en-GB', nb: 'nb-NO' }
const STORAGE_KEY = 'plass.language'

/** Allowed ranges of number fields, mirroring the @Min/@Max constraints in the backend. */
const FIELD_RANGES: Record<string, [number, number]> = {
  rows: [1, 26],
  seatsPerRow: [1, 40],
}

interface I18nValue {
  language: Language
  setLanguage: (language: Language) => void
  t: Messages
  f: Formatters
  /** A message for any error thrown by the API client, in the current language. */
  errorText: (error: unknown, fallback?: string) => string
  /** Per-field validation messages from a 400 response, in the current language. */
  fieldErrorsText: (error: unknown) => Record<string, string>
}

const I18nContext = createContext<I18nValue | null>(null)

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(initialLanguage)

  const setLanguage = useCallback((next: Language) => {
    setLanguageState(next)
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {
      // storage can be unavailable (private mode); the choice then lasts for this visit only
    }
  }, [])

  const t = messages[language]

  useEffect(() => {
    document.documentElement.lang = language
    document.title = t.meta.title
  }, [language, t])

  const value = useMemo<I18nValue>(() => {
    const errorText = (error: unknown, fallback?: string) => {
      if (!(error instanceof ApiError)) return fallback ?? t.errors.generic
      if (error.code === 'SEATS_UNAVAILABLE') return t.errors.SEATS_UNAVAILABLE(t.common.joinList(error.seats))
      if (error.code === 'NETWORK') return t.errors.network
      const known = error.code ? t.errors[error.code as keyof Messages['errors']] : undefined
      if (typeof known === 'string') return known
      if (error.status === 401) return t.errors.unauthorized
      // Unknown codes: the server's English text is better than nothing, but only in English.
      if (language === 'en' && error.message) return error.message
      return fallback ?? t.errors.generic
    }

    const fieldErrorsText = (error: unknown) => {
      if (!(error instanceof ApiError)) return {}
      const result: Record<string, string> = {}
      for (const [field, serverMessage] of Object.entries(error.fieldErrors)) {
        const code = error.fieldErrorCodes[field]
        result[field] = fieldMessage(t, field, code) ?? (language === 'en' ? serverMessage : t.fieldErrors.invalid)
      }
      return result
    }

    return { language, setLanguage, t, f: createFormatters(LOCALES[language]), errorText, fieldErrorsText }
  }, [language, setLanguage, t])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n() {
  const context = useContext(I18nContext)
  if (!context) throw new Error('useI18n must be used inside <I18nProvider>')
  return context
}

function fieldMessage(t: Messages, field: string, code: string | undefined) {
  switch (code) {
    case 'NotBlank':
    case 'NotNull':
    case 'NotEmpty':
      return t.fieldErrors.required
    case 'Email':
      return t.fieldErrors.email
    case 'Size':
      return field === 'password' ? t.fieldErrors.passwordLength : t.fieldErrors.tooLong
    case 'Pattern':
      return field === 'password' ? t.fieldErrors.passwordPattern : undefined
    case 'Future':
      return t.fieldErrors.future
    case 'DecimalMin':
      return t.fieldErrors.minPrice
    case 'Digits':
      return t.fieldErrors.digits
    case 'Min':
    case 'Max': {
      const range = FIELD_RANGES[field]
      return range ? t.fieldErrors.range(...range) : undefined
    }
    default:
      return undefined
  }
}

/** A saved choice wins; otherwise Norwegian browsers (nb, nn, no) get Norwegian and everyone else English. */
function initialLanguage(): Language {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved === 'en' || saved === 'nb') return saved
  } catch {
    // ignore unavailable storage
  }
  const preferred = typeof navigator === 'undefined' ? [] : navigator.languages ?? [navigator.language]
  return preferred.some((tag) => /^(nb|nn|no)\b/i.test(tag)) ? 'nb' : 'en'
}
