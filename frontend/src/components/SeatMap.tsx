import { useEffect, useMemo, useRef } from 'react'
import type { Seat, SeatTier } from '../api/types'
import { useI18n } from '../i18n/I18nContext'

interface SeatMapProps {
  seats: Seat[]
  prices: Record<SeatTier, number>
  selected: Set<number>
  onToggle: (seat: Seat) => void
  disabled?: boolean
}

/** The house plan: stage at the top, rows A, B, C... facing it, an aisle down the middle. */
export function SeatMap({ seats, prices, selected, onToggle, disabled }: SeatMapProps) {
  const { t, f } = useI18n()
  const rows = useMemo(() => {
    const byRow = new Map<string, Seat[]>()
    seats.forEach((seat) => byRow.set(seat.row, [...(byRow.get(seat.row) ?? []), seat]))
    return [...byRow.entries()].map(([row, rowSeats]) => ({
      row,
      seats: rowSeats.sort((a, b) => a.number - b.number),
    }))
  }, [seats])

  // On narrow screens the map scrolls sideways: start centred on the stage.
  const scroller = useRef<HTMLDivElement>(null)
  const hasSeats = seats.length > 0
  useEffect(() => {
    const el = scroller.current
    if (el && hasSeats) el.scrollLeft = (el.scrollWidth - el.clientWidth) / 2
  }, [hasSeats])

  const perRow = rows[0]?.seats.length ?? 0
  const aisleAfter = Math.ceil(perRow / 2)

  return (
    <div ref={scroller} className="overflow-x-auto pb-2">
      <div className="mx-auto w-max px-2">
        <div className="mx-auto mb-8 w-[85%]" aria-hidden>
          <svg viewBox="0 0 400 46" className="w-full">
            <path d="M8 6 Q200 58 392 6 L392 0 L8 0 Z" fill="var(--color-ink)" />
          </svg>
          <p className="-mt-1 text-center font-display text-lg font-semibold tracking-[0.12em] text-ink-soft">{t.seatMap.stage}</p>
        </div>

        <div role="group" aria-label={t.seatMap.label} className="space-y-1.5">
          {rows.map(({ row, seats: rowSeats }) => (
            <div key={row} className="flex items-center gap-1">
              <RowLabel row={row} />
              {rowSeats.map((seat, index) => {
                const isSelected = selected.has(seat.id)
                const unavailable = seat.status !== 'AVAILABLE' && !isSelected
                const description = `${t.common.seatName(seat.row, seat.number)}, ${t.tier[seat.tier]}, ${f.money(
                  prices[seat.tier],
                )}${unavailable ? `, ${t.seatMap.taken}` : ''}`
                return (
                  <span key={seat.id} className={index === aisleAfter ? 'ml-4' : undefined}>
                    <button
                      type="button"
                      className="seat"
                      data-tier={seat.tier}
                      data-status={isSelected ? 'AVAILABLE' : seat.status}
                      aria-pressed={isSelected}
                      aria-label={description}
                      title={description}
                      disabled={unavailable || disabled}
                      onClick={() => onToggle(seat)}
                    >
                      {seat.number}
                    </button>
                  </span>
                )
              })}
              <RowLabel row={row} />
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function RowLabel({ row }: { row: string }) {
  return (
    <span className="w-6 text-center font-display text-lg font-bold text-ink-soft" aria-hidden>
      {row}
    </span>
  )
}

export function SeatLegend({ prices }: { prices: Record<SeatTier, number> }) {
  const { t, f } = useI18n()
  const items: { key: string; label: string; tier?: SeatTier; status?: string; pressed?: boolean }[] = [
    { key: 'vip', label: `${t.tier.VIP} ${f.money(prices.VIP)}`, tier: 'VIP' },
    { key: 'premium', label: `${t.tier.PREMIUM} ${f.money(prices.PREMIUM)}`, tier: 'PREMIUM' },
    { key: 'standard', label: `${t.tier.STANDARD} ${f.money(prices.STANDARD)}`, tier: 'STANDARD' },
    { key: 'mine', label: t.seatMap.yourSeats, pressed: true },
    { key: 'held', label: t.seatMap.beingBooked, status: 'HELD' },
    { key: 'sold', label: t.seatMap.sold, status: 'BOOKED' },
  ]
  return (
    <ul className="flex flex-wrap gap-x-5 gap-y-2 text-sm">
      {items.map((item) => (
        <li key={item.key} className="flex items-center gap-2">
          <span
            className="seat inline-block scale-75"
            data-tier={item.tier}
            data-status={item.status}
            data-selected={item.pressed}
            aria-hidden
          />
          {item.label}
        </li>
      ))}
    </ul>
  )
}
