package com.plass.booking;

/**
 * PENDING   seats are held, waiting for payment (until expiresAt)
 * CONFIRMED paid, seats are BOOKED
 * CANCELLED cancelled by the user or because the event was cancelled
 * EXPIRED   not paid in time, seats were released automatically
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}
