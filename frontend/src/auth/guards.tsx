import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { PageLoader } from '../components/Feedback'
import { useAuth } from './AuthContext'

/** Sends visitors to the login page and brings them back afterwards. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, restoring } = useAuth()
  const location = useLocation()
  if (restoring) return <PageLoader />
  if (!user) {
    const next = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?next=${next}`} replace />
  }
  return children
}

export function RequireAdmin({ children }: { children: ReactNode }) {
  const { user, restoring } = useAuth()
  if (restoring) return <PageLoader />
  if (!user) return <Navigate to="/login?next=%2Fadmin" replace />
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />
  return children
}
