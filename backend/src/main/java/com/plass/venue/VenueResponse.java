package com.plass.venue;

public record VenueResponse(Long id, String name, String city, String address, int rows, int seatsPerRow,
        int capacity) {

    public static VenueResponse from(Venue venue) {
        return new VenueResponse(venue.getId(), venue.getName(), venue.getCity(), venue.getAddress(),
                venue.getRows(), venue.getSeatsPerRow(), venue.capacity());
    }
}
