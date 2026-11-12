import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { setUnauthorizedHandler, tokenStore } from '../api/client'
import { api } from '../api/endpoints'
import type { AuthResponse, User } from '../api/types'

interface AuthContextValue {
  user: User | null
  /** True until we know whether a stored token is still valid. */
  restoring: boolean
  login: (email: string, password: string) => Promise<User>
  register: (fullName: string, email: string, password: string) => Promise<User>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [restoring, setRestoring] = useState(() => tokenStore.get() !== null)

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!tokenStore.get()) return
    api.auth
      .me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setRestoring(false))
  }, [logout])

  const accept = useCallback((response: AuthResponse) => {
    tokenStore.set(response.token)
    setUser(response.user)
    return response.user
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      restoring,
      logout,
      login: (email, password) => api.auth.login(email, password).then(accept),
      register: (fullName, email, password) => api.auth.register(fullName, email, password).then(accept),
    }),
    [user, restoring, logout, accept],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
