package com.plass.event;

/** Aggregated seat numbers of one event, loaded with a single GROUP BY query. */
public record SeatCounts(Long eventId, Long total, Long available, Long held, Long booked) {

    public static SeatCounts empty(Long eventId) {
        return new SeatCounts(eventId, 0L, 0L, 0L, 0L);
    }
}
