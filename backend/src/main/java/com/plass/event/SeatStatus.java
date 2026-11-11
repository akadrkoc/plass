package com.plass.event;

/**
 * AVAILABLE → HELD (someone is paying, for a limited time) → BOOKED (paid).
 * A HELD seat goes back to AVAILABLE when the hold expires or is cancelled.
 */
public enum SeatStatus {
    AVAILABLE,
    HELD,
    BOOKED
}
