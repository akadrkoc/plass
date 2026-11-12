// Types mirror the JSON returned by the Spring Boot API (see the *Response records in the backend).

export type Role = 'USER' | 'ADMIN'
export type EventCategory = 'CONCERT' | 'THEATRE' | 'COMEDY' | 'CLASSICAL' | 'FESTIVAL' | 'KIDS'
export type EventStatus = 'PUBLISHED' | 'CANCELLED'
export type SeatTier = 'VIP' | 'PREMIUM' | 'STANDARD'
export type SeatStatus = 'AVAILABLE' | 'HELD' | 'BOOKED'
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED'

export interface User {
  id: number
  email: string
  fullName: string
  role: Role
}

export interface AuthResponse {
  token: string
  expiresAt: string
  user: User
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface EventSummary {
  id: number
  title: string
  category: EventCategory
  status: EventStatus
  startsAt: string
  venueName: string
  city: string
  priceFrom: number
  totalSeats: number
  availableSeats: number
}

export interface Venue {
  id: number
  name: string
  city: string
  address: string
  rows: number
  seatsPerRow: number
  capacity: number
}

export interface TierPrice {
  tier: SeatTier
  price: number
}

export interface EventDetail {
  id: number
  title: string
  description: string
  category: EventCategory
  status: EventStatus
  startsAt: string
  venue: Venue
  prices: TierPrice[]
  totalSeats: number
  availableSeats: number
  bookable: boolean
}

export interface Seat {
  id: number
  row: string
  number: number
  tier: SeatTier
  status: SeatStatus
}

export interface EventFilters {
  cities: string[]
  categories: EventCategory[]
}

export interface Booking {
  id: number
  reference: string
  status: BookingStatus
  totalAmount: number
  createdAt: string
  expiresAt: string
  confirmedAt: string | null
  cancelledAt: string | null
  cancellable: boolean
  event: {
    id: number
    title: string
    category: EventCategory
    startsAt: string
    venueName: string
    city: string
  }
  seats: { seatId: number; row: string; number: number; tier: SeatTier; price: number }[]
}

export interface AdminEvent {
  id: number
  title: string
  category: EventCategory
  status: EventStatus
  startsAt: string
  venueName: string
  city: string
  totalSeats: number
  bookedSeats: number
  heldSeats: number
  availableSeats: number
  revenue: number
}

export interface Dashboard {
  totalRevenue: number
  ticketsSold: number
  confirmedBookings: number
  upcomingEvents: number
  registeredUsers: number
  salesLast14Days: { date: string; tickets: number; revenue: number }[]
  recentBookings: {
    reference: string
    customerName: string
    eventTitle: string
    tickets: number
    amount: number
    confirmedAt: string
  }[]
}

export interface CreateEventInput {
  title: string
  description: string
  category: EventCategory
  venueId: number
  startsAt: string
  vipPrice: number
  premiumPrice: number
  standardPrice: number
}

export interface CreateVenueInput {
  name: string
  city: string
  address: string
  rows: number
  seatsPerRow: number
}
