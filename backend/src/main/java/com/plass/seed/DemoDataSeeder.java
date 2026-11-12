package com.plass.seed;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.plass.booking.Booking;
import com.plass.booking.BookingProperties;
import com.plass.booking.BookingReferenceGenerator;
import com.plass.booking.BookingRepository;
import com.plass.event.Event;
import com.plass.event.EventCategory;
import com.plass.event.EventRepository;
import com.plass.event.Seat;
import com.plass.event.SeatLayout;
import com.plass.event.SeatRepository;
import com.plass.user.Role;
import com.plass.user.User;
import com.plass.user.UserRepository;
import com.plass.venue.Venue;
import com.plass.venue.VenueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with realistic demo data: users, venues, upcoming events and
 * a few hundred paid bookings so the seat maps and the admin dashboard look alive.
 * Disable with SEED_DEMO_DATA=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingReferenceGenerator referenceGenerator;
    private final BookingProperties bookingProperties;
    private final Clock clock;

    private final Random random = new Random(42);
    private final Set<String> usedReferences = new HashSet<>();

    public DemoDataSeeder(UserRepository userRepository, VenueRepository venueRepository,
            EventRepository eventRepository, SeatRepository seatRepository, BookingRepository bookingRepository,
            PasswordEncoder passwordEncoder, BookingReferenceGenerator referenceGenerator,
            BookingProperties bookingProperties, Clock clock) {
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.referenceGenerator = referenceGenerator;
        this.bookingProperties = bookingProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data, skipping demo seed");
            return;
        }
        Instant now = Instant.now(clock);

        user("admin@plass.dev", "Admin123!", "Plass Admin", Role.ADMIN, now);
        User demo = user("demo@plass.dev", "Demo1234!", "Ola Nordmann", Role.USER, now);
        List<User> customers = List.of(
                user("ingrid.hansen@example.com", null, "Ingrid Hansen", Role.USER, now),
                user("lars.johansen@example.com", null, "Lars Johansen", Role.USER, now),
                user("sofie.olsen@example.com", null, "Sofie Olsen", Role.USER, now),
                user("magnus.larsen@example.com", null, "Magnus Larsen", Role.USER, now),
                user("emma.andersen@example.com", null, "Emma Andersen", Role.USER, now),
                user("henrik.berg@example.com", null, "Henrik Berg", Role.USER, now));

        Venue hall = venueRepository.save(new Venue("Oslo Konserthus", "Oslo", "Munkedamsveien 14, Vika", 14, 24, now));
        Venue blackBox = venueRepository.save(new Venue("Black Box Teater", "Oslo", "Marstrandgata 8, Grünerløkka", 8, 14, now));
        Venue oslomet = venueRepository.save(new Venue("OsloMet Auditorium", "Oslo", "Pilestredet, St. Hanshaugen", 10, 18, now));
        Venue grieg = venueRepository.save(new Venue("Grieghallen", "Bergen", "Edvard Griegs plass 1", 14, 22, now));
        Venue olavs = venueRepository.save(new Venue("Olavshallen", "Trondheim", "Kjøpmannsgata 44", 12, 20, now));

        Map<String, Event> events = new LinkedHashMap<>();
        events.put("jazz", event("Midnight Jazz Session", EventCategory.CONCERT, hall, 4, 21, 0, 790, 590, 390, 0.55,
                "A late-night journey through Nordic jazz with a quintet of Oslo's finest musicians. "
                        + "Expect improvised solos, soulful ballads and a few surprises.", now, customers));
        events.put("dollhouse", event("A Doll's House: A Modern Staging", EventCategory.THEATRE, blackBox, 6, 19, 30, 550, 420, 290, 0.7,
                "Ibsen's classic reimagined in a glass-walled flat in today's Oslo. "
                        + "An intimate black-box production with a cast of six.", now, customers));
        events.put("standup", event("Stand-up Night: Four Seasons of Rain", EventCategory.COMEDY, grieg, 3, 21, 0, 490, 390, 290, 0.8,
                "Ninety minutes of observational comedy about Bergen weather, dugnad and "
                        + "everything in between.", now, customers));
        events.put("swan", event("Swan Lake Ballet", EventCategory.CLASSICAL, hall, 12, 19, 0, 1190, 890, 590, 0.4,
                "Tchaikovsky's timeless ballet performed with a live symphony orchestra.", now, customers));
        events.put("indie", event("Indie Rock Night", EventCategory.CONCERT, olavs, 9, 21, 30, 520, 420, 320, 0.35,
                "Three up-and-coming bands from the Trondheim scene share one stage.", now, customers));
        events.put("puppet", event("The Troll Forest", EventCategory.KIDS, blackBox, 5, 12, 0, 250, 200, 150, 0.5,
                "A colourful puppet show for ages 4-10 about a forest troll who forgot how to sing.", now, customers));
        events.put("revue", event("OsloMet Student Revue", EventCategory.COMEDY, oslomet, 8, 19, 0, 250, 200, 150, 0.65,
                "OsloMet students roast exam season, the canteen queue and life at Pilestredet "
                        + "in this year's sketch revue.", now, customers));
        events.put("christmas", event("OsloMet Christmas Concert", EventCategory.CONCERT, oslomet, 18, 18, 0, 300, 250, 200, 0.3,
                "The OsloMet choir and student band open the Christmas season with carols old and new.", now, customers));
        events.put("folk", event("Nordic Folk Evening", EventCategory.CONCERT, grieg, 15, 19, 30, 590, 450, 320, 0.3,
                "Traditional tunes from the fjords and mountains with Hardanger fiddle, kveding and a full choir.", now, customers));
        events.put("drama", event("OsloMet Drama Showcase", EventCategory.THEATRE, oslomet, 11, 18, 30, 220, 180, 120, 0.45,
                "Graduating drama and theatre students from OsloMet present three short new plays.", now, customers));
        events.put("festival", event("Northern Lights Electronic Festival", EventCategory.FESTIVAL, olavs, 20, 18, 0, 990, 790, 590, 0.2,
                "Six hours of electronic music with local and international DJs on the longest nights of the year.", now, customers));
        events.put("ninth", event("Beethoven Symphony No. 9", EventCategory.CLASSICAL, grieg, 25, 19, 30, 890, 650, 450, 0.15,
                "The Ode to Joy, performed by a 90-piece orchestra and choir.", now, customers));
        events.put("pastJazz", event("Autumn Jazz Weekend", EventCategory.CONCERT, hall, -2, 20, 0, 690, 520, 350, 0.9,
                "Two nights of swing and bebop. This event has already taken place.", now, customers));

        // The demo account gets a few tickets so "My tickets" isn't empty.
        demoBooking(demo, events.get("jazz"), "D", 2, now.minus(Duration.ofDays(2)), false);
        demoBooking(demo, events.get("swan"), "B", 3, now.minus(Duration.ofDays(1)), false);
        demoBooking(demo, events.get("indie"), "F", 2, now.minus(Duration.ofDays(5)), true);

        log.info("Demo data created: {} venues, {} events, {} bookings. Log in as demo@plass.dev / Demo1234! "
                + "or admin@plass.dev / Admin123!", venueRepository.count(), events.size(), bookingRepository.count());
    }

    private User user(String email, String password, String fullName, Role role, Instant now) {
        String rawPassword = password != null ? password : UUID.randomUUID().toString();
        return userRepository.save(new User(email, passwordEncoder.encode(rawPassword), fullName, role, now));
    }

    /** Creates an event plus paid bookings for roughly {@code occupancy} of its seats. */
    private Event event(String title, EventCategory category, Venue venue, int daysFromToday, int hour, int minute,
            int vip, int premium, int standard, double occupancy, String description, Instant now,
            List<User> customers) {
        Instant startsAt = LocalDate.ofInstant(now, ZONE).plusDays(daysFromToday)
                .atTime(LocalTime.of(hour, minute)).atZone(ZONE).toInstant();
        Event event = eventRepository.save(new Event(title, description, category, venue, startsAt,
                BigDecimal.valueOf(vip), BigDecimal.valueOf(premium), BigDecimal.valueOf(standard),
                now.minus(Duration.ofDays(30))));
        List<Seat> seats = seatRepository.saveAll(SeatLayout.generate(event, venue));

        // Sales happened during the last 14 days (and before the event, for past events).
        Instant salesEnd = startsAt.isBefore(now) ? startsAt.minus(Duration.ofHours(3)) : now.minus(Duration.ofMinutes(30));
        Instant salesStart = salesEnd.minus(Duration.ofDays(13));

        Map<String, List<Seat>> rows = seats.stream()
                .collect(Collectors.groupingBy(Seat::getRowLabel, LinkedHashMap::new, Collectors.toList()));
        for (List<Seat> row : rows.values()) {
            int index = 0;
            while (index < row.size()) {
                int groupSize = Math.min(1 + random.nextInt(4), row.size() - index);
                List<Seat> group = row.subList(index, index + groupSize);
                if (random.nextDouble() < occupancy) {
                    User customer = customers.get(random.nextInt(customers.size()));
                    paidBooking(customer, event, new ArrayList<>(group), randomInstant(salesStart, salesEnd));
                }
                index += groupSize;
            }
        }
        return event;
    }

    private void demoBooking(User user, Event event, String row, int seatCount, Instant paidAt, boolean cancelLater) {
        List<Seat> rowSeats = seatRepository.findByEventIdOrderByRowLabelAscSeatNumberAsc(event.getId()).stream()
                .filter(seat -> seat.getRowLabel().equals(row))
                .toList();
        findAdjacentAvailable(rowSeats, seatCount).ifPresent(seats -> {
            Booking booking = paidBooking(user, event, seats, paidAt);
            if (cancelLater) {
                booking.cancel(paidAt.plus(Duration.ofDays(1)));
            }
        });
    }

    private Booking paidBooking(User user, Event event, List<Seat> seats, Instant paidAt) {
        Booking booking = Booking.hold(uniqueReference(), user, event, seats, paidAt.minus(Duration.ofMinutes(3)),
                bookingProperties.holdDuration());
        booking.confirm("SEED-" + UUID.randomUUID().toString().substring(0, 8), paidAt);
        return bookingRepository.save(booking);
    }

    private static Optional<List<Seat>> findAdjacentAvailable(List<Seat> rowSeats, int count) {
        for (int start = 0; start + count <= rowSeats.size(); start++) {
            List<Seat> candidate = rowSeats.subList(start, start + count);
            if (candidate.stream().allMatch(Seat::isAvailable)) {
                return Optional.of(new ArrayList<>(candidate));
            }
        }
        return Optional.empty();
    }

    private Instant randomInstant(Instant from, Instant to) {
        long span = Math.max(1, Duration.between(from, to).getSeconds());
        return from.plusSeconds((long) (random.nextDouble() * span));
    }

    private String uniqueReference() {
        String reference;
        do {
            reference = referenceGenerator.next();
        }
        while (!usedReferences.add(reference));
        return reference;
    }
}
