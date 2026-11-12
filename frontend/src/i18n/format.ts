// Events take place in Norway, so times are always shown in Oslo time and prices in kroner,
// whatever the visitor's own time zone is. Only the language of the formatting changes.
export const TIME_ZONE = 'Europe/Oslo'
export const CURRENCY = 'NOK'

export function createFormatters(locale: string) {
  const money = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: CURRENCY,
    currencyDisplay: 'narrowSymbol',
    maximumFractionDigits: 0,
  })
  const number = new Intl.NumberFormat(locale)

  const part = (date: Date, options: Intl.DateTimeFormatOptions) =>
    new Intl.DateTimeFormat(locale, { timeZone: TIME_ZONE, ...options }).format(date)
  const time = (date: Date) => part(date, { hour: '2-digit', minute: '2-digit', hour12: false })

  return {
    money: (amount: number) => money.format(amount),
    number: (value: number) => number.format(value),

    /** Pieces for the big date blocks. Norwegian writes "13." for a day on its own; the block drops the dot. */
    dateParts(iso: string) {
      const date = new Date(iso)
      return {
        day: part(date, { day: 'numeric' }).replace(/\.$/, ''),
        month: part(date, { month: 'short' }).replace(/\.$/, ''),
        weekday: part(date, { weekday: 'short' }).replace(/\.$/, ''),
        time: time(date),
      }
    },

    /** "Friday, 13 November 2026, 21:00" / "fredag 13. november 2026, 21:00" */
    longDate(iso: string) {
      const date = new Date(iso)
      return `${part(date, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}, ${time(date)}`
    },

    /** "13 Nov, 21:00" / "13. nov., 21:00" */
    shortDate(iso: string) {
      const date = new Date(iso)
      return `${part(date, { day: 'numeric', month: 'short' })}, ${time(date)}`
    },

    /** Formats a calendar day such as "2026-11-13" (no time, no zone shift). */
    day(isoDate: string, options: Intl.DateTimeFormatOptions) {
      return new Intl.DateTimeFormat(locale, { timeZone: 'UTC', ...options }).format(new Date(`${isoDate}T00:00:00Z`))
    },
  }
}

export type Formatters = ReturnType<typeof createFormatters>
