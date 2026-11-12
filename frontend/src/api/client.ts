// Small fetch wrapper: adds the JWT, parses RFC 9457 problem details into ApiError.

const TOKEN_KEY = 'plass.token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

let onUnauthorized: () => void = () => {}

/** Lets the auth layer react when the server rejects our token (e.g. it expired). */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly fieldErrors: Record<string, string>
  /** The constraint that failed per field, e.g. { email: 'Email' }; used to translate field errors. */
  readonly fieldErrorCodes: Record<string, string>
  readonly seats: string[]

  constructor(status: number, message: string, body?: Record<string, unknown>) {
    super(message)
    this.status = status
    this.code = typeof body?.code === 'string' ? body.code : undefined
    this.fieldErrors = (body?.errors as Record<string, string>) ?? {}
    this.fieldErrorCodes = (body?.errorCodes as Record<string, string>) ?? {}
    this.seats = (body?.seats as string[]) ?? []
  }
}

type Method = 'GET' | 'POST'

async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = tokenStore.get()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Could not reach the server. Check your connection and try again.', { code: 'NETWORK' })
  }

  if (response.status === 401 && token) {
    onUnauthorized()
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => undefined)
    const message =
      (problem && (problem.detail as string)) ||
      (response.status === 401 ? 'Please log in to continue.' : 'Something went wrong. Please try again.')
    throw new ApiError(response.status, message, problem)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export const http = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
}
