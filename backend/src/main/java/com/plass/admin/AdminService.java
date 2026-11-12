package com.plass.admin;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.plass.booking.BookingRepository;
import com.plass.booking.BookingService;
import com.plass.booking.BookingStatus;
import com.plass.booking.EventRevenue;
import com.plass.booking.SaleLine;
import com.plass.common.ApiException;
import com.plass.common.PageResponse;
import com.plass.event.CreateEventRequest;
import com.plass.event.Event;
import com.plass.event.EventRepository;
import com.plass.event.EventService;
import com.plass.event.EventStatus;
import com.plass.event.SeatCounts;
import com.plass.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    /** Sales are grouped per calendar day in Norway's time zone. */
    static final ZoneId REPORT_ZONE = ZoneId.of("Europe/Oslo");
    private static final int CHART_DAYS = 14;

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final Clock clock;

    public AdminService(EventRepository eventRepository, EventService eventService,
            BookingRepository bookingRepository, BookingService bookingService, UserRepository userRepository,
            Clock clock) {
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminEventResponse> listEvents(int page, int size) {
        Page<Event> events = eventRepository.findAllBy(
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by("startsAt").descending()));
        List<Long> ids = events.getContent().stream().map(Event::getId).toList();
        Map<Long, SeatCounts> counts = eventService.seatCountsFor(ids);
        Map<Long, BigDecimal> revenue = revenueFor(ids);
        return PageResponse.of(events, event -> AdminEventResponse.from(event,
                counts.getOrDefault(event.getId(), SeatCounts.empty(event.getId())),
                revenue.getOrDefault(event.getId(), BigDecimal.ZERO)));
    }

    @Transactional
    public AdminEventResponse createEvent(CreateEventRequest request) {
        Event event = eventService.create(request);
        return toAdminResponse(event);
    }

    /** Cancels the event and every active booking for it (paid ones are refunded). */
    @Transactional
    public AdminEventResponse cancelEvent(Long eventId) {
        Event event = eventRepository.findWithVenueById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event", eventId));
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw ApiException.conflict("EVENT_ALREADY_CANCELLED", "This event is already cancelled");
        }
        event.cancel();
        bookingService.cancelActiveBookingsForEvent(eventId);
        return toAdminResponse(event);
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Instant now = Instant.now(clock);
        LocalDate today = LocalDate.ofInstant(now, REPORT_ZONE);
        LocalDate firstDay = today.minusDays(CHART_DAYS - 1);

        Map<LocalDate, List<SaleLine>> salesByDay = bookingRepository
                .findSaleLinesSince(firstDay.atStartOfDay(REPORT_ZONE).toInstant()).stream()
                .collect(Collectors.groupingBy(line -> LocalDate.ofInstant(line.confirmedAt(), REPORT_ZONE)));

        List<DashboardResponse.DailySales> daily = new ArrayList<>(CHART_DAYS);
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            List<SaleLine> lines = salesByDay.getOrDefault(day, List.of());
            BigDecimal revenue = lines.stream().map(SaleLine::price).reduce(BigDecimal.ZERO, BigDecimal::add);
            daily.add(new DashboardResponse.DailySales(day, lines.size(), revenue));
        }

        List<DashboardResponse.RecentBooking> recent = bookingRepository
                .findTop8ByStatusOrderByConfirmedAtDesc(BookingStatus.CONFIRMED).stream()
                .map(booking -> new DashboardResponse.RecentBooking(
                        booking.getReference(),
                        booking.getUser().getFullName(),
                        booking.getEvent().getTitle(),
                        booking.getItems().size(),
                        booking.getTotalAmount(),
                        booking.getConfirmedAt()))
                .toList();

        return new DashboardResponse(
                bookingRepository.totalConfirmedRevenue(),
                bookingRepository.countSoldTickets(),
                bookingRepository.countByStatus(BookingStatus.CONFIRMED),
                eventRepository.countByStatusAndStartsAtAfter(EventStatus.PUBLISHED, now),
                userRepository.count(),
                daily,
                recent);
    }

    private AdminEventResponse toAdminResponse(Event event) {
        List<Long> ids = List.of(event.getId());
        return AdminEventResponse.from(event,
                eventService.seatCountsFor(ids).getOrDefault(event.getId(), SeatCounts.empty(event.getId())),
                revenueFor(ids).getOrDefault(event.getId(), BigDecimal.ZERO));
    }

    private Map<Long, BigDecimal> revenueFor(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return bookingRepository.revenueByEventIds(eventIds).stream()
                .collect(Collectors.toMap(EventRevenue::eventId, EventRevenue::revenue));
    }
}
