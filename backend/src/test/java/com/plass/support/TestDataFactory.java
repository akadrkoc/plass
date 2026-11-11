package com.plass.support;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.plass.auth.TokenService;
import com.plass.event.CreateEventRequest;
import com.plass.event.Event;
import com.plass.event.EventCategory;
import com.plass.event.EventService;
import com.plass.event.Seat;
import com.plass.event.SeatRepository;
import com.plass.user.Role;
import com.plass.user.User;
import com.plass.user.UserRepository;
import com.plass.venue.Venue;
import com.plass.venue.VenueRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Creates users, venues and events for integration tests. Every call creates fresh, unique data. */
public class TestDataFactory {

    public static final String PASSWORD = "Password1";

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final EventService eventService;
    private final TokenService tokenService;
    private final String passwordHash;

    public TestDataFactory(UserRepository userRepository, VenueRepository venueRepository,
            SeatRepository seatRepository, EventService eventService, TokenService tokenService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.seatRepository = seatRepository;
        this.eventService = eventService;
        this.tokenService = tokenService;
        this.passwordHash = passwordEncoder.encode(PASSWORD);
    }

    public User user() {
        return createUser(Role.USER);
    }

    public User admin() {
        return createUser(Role.ADMIN);
    }

    /** An event in 10 days at a 3 x 4 venue (row A = VIP, B = PREMIUM, C = STANDARD). */
    public Event upcomingEvent() {
        return eventStartingIn(Duration.ofDays(10));
    }

    public Event eventStartingIn(Duration startsIn) {
        Venue venue = venueRepository.save(new Venue("Test Hall", "Testville", "Main Street 1", 3, 4, Instant.now()));
        return eventService.create(new CreateEventRequest(
                "Test Event " + UUID.randomUUID(),
                "An event created by a test",
                EventCategory.CONCERT,
                venue.getId(),
                Instant.now().plus(startsIn),
                new BigDecimal("300.00"),
                new BigDecimal("200.00"),
                new BigDecimal("100.00")));
    }

    public List<Seat> seatsOf(Event event) {
        return seatRepository.findByEventIdOrderByRowLabelAscSeatNumberAsc(event.getId());
    }

    public List<Long> seatIds(Event event, String... labels) {
        List<String> wanted = List.of(labels);
        return seatsOf(event).stream().filter(seat -> wanted.contains(seat.label())).map(Seat::getId).toList();
    }

    public String bearerToken(User user) {
        return "Bearer " + tokenService.issue(user).value();
    }

    private User createUser(Role role) {
        String email = role.name().toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "@test.dev";
        return userRepository.save(new User(email, passwordHash, "Test " + role.name(), role, Instant.now()));
    }
}
