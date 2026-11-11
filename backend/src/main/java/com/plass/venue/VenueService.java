package com.plass.venue;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final Clock clock;

    public VenueService(VenueRepository venueRepository, Clock clock) {
        this.venueRepository = venueRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> findAll() {
        return venueRepository.findAllByOrderByCityAscNameAsc().stream().map(VenueResponse::from).toList();
    }

    @Transactional
    public VenueResponse create(CreateVenueRequest request) {
        Venue venue = venueRepository.save(new Venue(
                request.name().trim(),
                request.city().trim(),
                request.address().trim(),
                request.rows(),
                request.seatsPerRow(),
                Instant.now(clock)));
        return VenueResponse.from(venue);
    }
}
