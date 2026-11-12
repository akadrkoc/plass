import { useEffect, useState } from 'react'

/** Live mm:ss countdown to `until`. Calls onExpire once when it reaches zero. */
export function useCountdown(until: string | undefined, onExpire?: () => void) {
  const [now, setNow] = useState(() => Date.now())
  const target = until ? new Date(until).getTime() : 0
  const remainingMs = Math.max(0, target - now)

  useEffect(() => {
    if (!until) return
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [until])

  const expired = Boolean(until) && remainingMs === 0
  useEffect(() => {
    if (expired) onExpire?.()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [expired])

  const minutes = Math.floor(remainingMs / 60_000)
  const seconds = Math.floor((remainingMs % 60_000) / 1000)
  return {
    remainingMs,
    expired,
    text: `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`,
  }
}
