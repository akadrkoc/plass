import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '../api/client'

interface State<T> {
  data: T | undefined
  error: ApiError | undefined
  loading: boolean
}

/**
 * Loads data on mount and again whenever `key` changes (e.g. `event:42`).
 * `reload()` fetches again in the background, keeping the current data on screen (used for polling).
 */
export function useApi<T>(key: string, load: () => Promise<T>) {
  const loadRef = useRef(load)
  useEffect(() => {
    loadRef.current = load
  })

  const [state, setState] = useState<State<T>>({ data: undefined, error: undefined, loading: true })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let active = true
    loadRef.current()
      .then((data) => {
        if (active) setState({ data, error: undefined, loading: false })
      })
      .catch((error: unknown) => {
        if (!active) return
        const apiError = error instanceof ApiError ? error : new ApiError(0, 'Something went wrong.')
        setState((previous) => ({ ...previous, error: apiError, loading: false }))
      })
    return () => {
      active = false
    }
  }, [key, version])

  const reload = useCallback(() => setVersion((v) => v + 1), [])
  const setData = useCallback((data: T) => setState({ data, error: undefined, loading: false }), [])

  return { ...state, reload, setData }
}

/** The time the component first rendered; stable across re-renders. */
export function useNow() {
  const [now] = useState(Date.now)
  return now
}
