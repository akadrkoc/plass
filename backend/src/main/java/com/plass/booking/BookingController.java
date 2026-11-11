package com.plass.booking;

import java.util.List;

import com.plass.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Hold seats, pay and manage your tickets")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Hold seats for a limited time (step 1 of checkout)")
    public BookingResponse hold(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody HoldSeatsRequest request) {
        return bookingService.holdSeats(userId(jwt), request);
    }

    @GetMapping
    @Operation(summary = "My bookings, newest first")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.findMyBookings(userId(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One of my bookings")
    public BookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.findMyBooking(userId(jwt), id);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay for a held booking (simulated payment, step 2 of checkout)")
    public BookingResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.pay(userId(jwt), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a held or paid booking")
    public BookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.cancel(userId(jwt), id);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
