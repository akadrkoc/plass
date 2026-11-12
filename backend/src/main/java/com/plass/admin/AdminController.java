package com.plass.admin;

import java.util.List;

import com.plass.common.PageResponse;
import com.plass.config.OpenApiConfig;
import com.plass.event.CreateEventRequest;
import com.plass.venue.CreateVenueRequest;
import com.plass.venue.VenueResponse;
import com.plass.venue.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Organiser endpoints. Protected by hasRole("ADMIN") in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Organiser tools (ADMIN role)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminController {

    private final AdminService adminService;
    private final VenueService venueService;

    public AdminController(AdminService adminService, VenueService venueService) {
        this.adminService = adminService;
        this.venueService = venueService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Sales KPIs, last 14 days of sales and recent bookings")
    public DashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/events")
    @Operation(summary = "All events (incl. past and cancelled) with occupancy and revenue")
    public PageResponse<AdminEventResponse> events(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listEvents(page, size);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an event; seats are generated from the venue plan")
    public AdminEventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return adminService.createEvent(request);
    }

    @PostMapping("/events/{id}/cancel")
    @Operation(summary = "Cancel an event and refund all its bookings")
    public AdminEventResponse cancelEvent(@PathVariable Long id) {
        return adminService.cancelEvent(id);
    }

    @GetMapping("/venues")
    @Operation(summary = "All venues")
    public List<VenueResponse> venues() {
        return venueService.findAll();
    }

    @PostMapping("/venues")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a venue with a rectangular seating plan")
    public VenueResponse createVenue(@Valid @RequestBody CreateVenueRequest request) {
        return venueService.create(request);
    }
}
