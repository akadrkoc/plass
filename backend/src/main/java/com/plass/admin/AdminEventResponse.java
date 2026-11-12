package com.plass.admin;

import java.math.BigDecimal;
import java.time.Instant;

import com.plass.event.Event;
import com.plass.event.EventCategory;
import com.plass.event.EventStatus;
import com.plass.event.SeatCounts;

public record AdminEventResponse(
        Long id,
        String title,
        EventCategory category,
        EventStatus status,
        Instant startsAt,
        String venueName,
        String city,
        long totalSeats,
        long bookedSeats,
        long heldSeats,
        long availableSeats,
        BigDecimal revenue) {

    static AdminEventResponse from(Event event, SeatCounts counts, BigDecimal revenue) {
        return new AdminEventResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getStatus(),
                event.getStartsAt(),
                event.getVenue().getName(),
                event.getVenue().getCity(),
                counts.total(),
                counts.booked(),
                counts.held(),
                counts.available(),
                revenue);
    }
}
