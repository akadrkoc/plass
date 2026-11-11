package com.plass.event;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.plass.venue.VenueResponse;

public record EventDetailResponse(
        Long id,
        String title,
        String description,
        EventCategory category,
        EventStatus status,
        Instant startsAt,
        VenueResponse venue,
        List<TierPriceResponse> prices,
        long totalSeats,
        long availableSeats,
        boolean bookable) {

    public static EventDetailResponse from(Event event, SeatCounts counts, Instant now) {
        List<TierPriceResponse> prices = Arrays.stream(SeatTier.values())
                .map(tier -> new TierPriceResponse(tier, event.priceFor(tier)))
                .toList();
        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getStatus(),
                event.getStartsAt(),
                VenueResponse.from(event.getVenue()),
                prices,
                counts.total(),
                counts.available(),
                event.isOpenForBooking(now));
    }
}
