import { http } from './client'
import type {
  AdminEvent,
  AuthResponse,
  Booking,
  CreateEventInput,
  CreateVenueInput,
  Dashboard,
  EventCategory,
  EventDetail,
  EventFilters,
  EventSummary,
  Page,
  Seat,
  User,
  Venue,
} from './types'

export interface EventQuery {
  q?: string
  city?: string
  category?: EventCategory
  page?: number
  size?: number
}

function query(params: Record<string, string | number | undefined>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') search.set(key, String(value))
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}

export const api = {
  auth: {
    login: (email: string, password: string) => http.post<AuthResponse>('/auth/login', { email, password }),
    register: (fullName: string, email: string, password: string) =>
      http.post<AuthResponse>('/auth/register', { fullName, email, password }),
    me: () => http.get<User>('/auth/me'),
  },
  events: {
    search: (params: EventQuery) => http.get<Page<EventSummary>>(`/events${query({ ...params })}`),
    filters: () => http.get<EventFilters>('/events/filters'),
    get: (id: number) => http.get<EventDetail>(`/events/${id}`),
    seats: (id: number) => http.get<Seat[]>(`/events/${id}/seats`),
  },
  bookings: {
    hold: (eventId: number, seatIds: number[]) => http.post<Booking>('/bookings', { eventId, seatIds }),
    mine: () => http.get<Booking[]>('/bookings'),
    get: (id: number) => http.get<Booking>(`/bookings/${id}`),
    pay: (id: number) => http.post<Booking>(`/bookings/${id}/pay`),
    cancel: (id: number) => http.post<Booking>(`/bookings/${id}/cancel`),
  },
  admin: {
    dashboard: () => http.get<Dashboard>('/admin/dashboard'),
    events: (page = 0, size = 50) => http.get<Page<AdminEvent>>(`/admin/events${query({ page, size })}`),
    createEvent: (input: CreateEventInput) => http.post<AdminEvent>('/admin/events', input),
    cancelEvent: (id: number) => http.post<AdminEvent>(`/admin/events/${id}/cancel`),
    venues: () => http.get<Venue[]>('/admin/venues'),
    createVenue: (input: CreateVenueInput) => http.post<Venue>('/admin/venues', input),
  },
}
