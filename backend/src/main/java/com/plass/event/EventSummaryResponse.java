package com.plass.event;

import java.math.BigDecimal;
import java.time.Instant;

public record EventSummaryResponse(
        Long id,
        String title,
        EventCategory category,
        EventStatus status,
        Instant startsAt,
        String venueName,
        String city,
        BigDecimal priceFrom,
        long totalSeats,
        long availableSeats) {

    public static EventSummaryResponse from(Event event, SeatCounts counts) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getStatus(),
                event.getStartsAt(),
                event.getVenue().getName(),
                event.getVenue().getCity(),
                event.lowestPrice(),
                counts.total(),
                counts.available());
    }
}
