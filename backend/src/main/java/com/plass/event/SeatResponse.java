package com.plass.event;

public record SeatResponse(Long id, String row, int number, SeatTier tier, SeatStatus status) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(), seat.getTier(),
                seat.getStatus());
    }
}
