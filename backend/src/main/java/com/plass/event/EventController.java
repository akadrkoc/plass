package com.plass.event;

import java.util.List;

import com.plass.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Public event catalogue")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Search upcoming events")
    public PageResponse<EventSummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return eventService.search(q, city, category, page, size);
    }

    @GetMapping("/filters")
    @Operation(summary = "Cities and categories to filter by")
    public EventFiltersResponse filters() {
        return eventService.getFilters();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Event details with prices and availability")
    public EventDetailResponse get(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @GetMapping("/{id}/seats")
    @Operation(summary = "Seat map of an event")
    public List<SeatResponse> seats(@PathVariable Long id) {
        return eventService.getSeats(id);
    }
}
