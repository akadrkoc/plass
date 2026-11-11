package com.plass.event;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.plass.common.ApiException;
import com.plass.common.PageResponse;
import com.plass.venue.Venue;
import com.plass.venue.VenueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EventService {

    private static final int MAX_PAGE_SIZE = 50;

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final VenueRepository venueRepository;
    private final Clock clock;

    public EventService(EventRepository eventRepository, SeatRepository seatRepository,
            VenueRepository venueRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.venueRepository = venueRepository;
        this.clock = clock;
    }

    /** Public search: only upcoming, published events, soonest first. */
    @Transactional(readOnly = true)
    public PageResponse<EventSummaryResponse> search(String text, String city, EventCategory category,
            int page, int size) {
        List<Specification<Event>> filters = new ArrayList<>();
        filters.add(EventSpecifications.isPublished());
        filters.add(EventSpecifications.startsAfter(Instant.now(clock)));
        if (StringUtils.hasText(text)) {
            filters.add(EventSpecifications.matchesText(text));
        }
        if (StringUtils.hasText(city)) {
            filters.add(EventSpecifications.inCity(city));
        }
        if (category != null) {
            filters.add(EventSpecifications.hasCategory(category));
        }

        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by("startsAt").ascending().and(Sort.by("id")));
        Page<Event> events = eventRepository.findAll(Specification.allOf(filters), pageRequest);

        Map<Long, SeatCounts> counts = seatCountsFor(events.getContent().stream().map(Event::getId).toList());
        return PageResponse.of(events,
                event -> EventSummaryResponse.from(event, counts.getOrDefault(event.getId(), SeatCounts.empty(event.getId()))));
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getEvent(Long id) {
        Event event = eventRepository.findWithVenueById(id).orElseThrow(() -> ApiException.notFound("Event", id));
        SeatCounts counts = seatCountsFor(List.of(id)).getOrDefault(id, SeatCounts.empty(id));
        return EventDetailResponse.from(event, counts, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw ApiException.notFound("Event", eventId);
        }
        return seatRepository.findByEventIdOrderByRowLabelAscSeatNumberAsc(eventId).stream()
                .map(SeatResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventFiltersResponse getFilters() {
        return new EventFiltersResponse(
                eventRepository.findCitiesWithUpcomingEvents(Instant.now(clock)),
                Arrays.asList(EventCategory.values()));
    }

    /** Creates an event and generates one seat row per venue row. */
    @Transactional
    public Event create(CreateEventRequest request) {
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> ApiException.notFound("Venue", request.venueId()));
        Event event = eventRepository.save(new Event(
                request.title().trim(),
                request.description().trim(),
                request.category(),
                venue,
                request.startsAt(),
                request.vipPrice(),
                request.premiumPrice(),
                request.standardPrice(),
                Instant.now(clock)));
        seatRepository.saveAll(SeatLayout.generate(event, venue));
        return event;
    }

    @Transactional(readOnly = true)
    public Map<Long, SeatCounts> seatCountsFor(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return seatRepository.countByEventIds(eventIds).stream()
                .collect(Collectors.toMap(SeatCounts::eventId, Function.identity()));
    }
}
