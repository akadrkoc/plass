package com.plass.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.plass.event.Event;
import com.plass.event.EventCategory;
import com.plass.event.Seat;
import com.plass.event.SeatTier;

public record BookingResponse(
        Long id,
        String reference,
        BookingStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant expiresAt,
        Instant confirmedAt,
        Instant cancelledAt,
        boolean cancellable,
        EventInfo event,
        List<SeatInfo> seats) {

    public record EventInfo(Long id, String title, EventCategory category, Instant startsAt, String venueName,
            String city) {
    }

    public record SeatInfo(Long seatId, String row, int number, SeatTier tier, BigDecimal price) {
    }

    static BookingResponse from(Booking booking, boolean cancellable) {
        Event event = booking.getEvent();
        EventInfo eventInfo = new EventInfo(event.getId(), event.getTitle(), event.getCategory(),
                event.getStartsAt(), event.getVenue().getName(), event.getVenue().getCity());
        List<SeatInfo> seats = booking.getItems().stream()
                .map(item -> {
                    Seat seat = item.getSeat();
                    return new SeatInfo(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(), seat.getTier(),
                            item.getPrice());
                })
                .toList();
        return new BookingResponse(
                booking.getId(),
                booking.getReference(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getCreatedAt(),
                booking.getExpiresAt(),
                booking.getConfirmedAt(),
                booking.getCancelledAt(),
                cancellable,
                eventInfo,
                seats);
    }
}
