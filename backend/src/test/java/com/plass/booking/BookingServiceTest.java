package com.plass.booking;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.plass.common.ApiException;
import com.plass.event.Event;
import com.plass.event.EventCategory;
import com.plass.event.EventRepository;
import com.plass.event.Seat;
import com.plass.event.SeatLayout;
import com.plass.event.SeatRepository;
import com.plass.payment.PaymentGateway;
import com.plass.payment.PaymentResult;
import com.plass.user.Role;
import com.plass.user.User;
import com.plass.user.UserRepository;
import com.plass.venue.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Unit tests of BookingService with mocked repositories (fast, no Docker needed). */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long EVENT_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private BookingReferenceGenerator referenceGenerator;

    private BookingService service;
    private Venue venue;
    private Event event;
    private List<Seat> seats;

    @BeforeEach
    void setUp() {
        BookingProperties properties = new BookingProperties(Duration.ofMinutes(10), 6, Duration.ofHours(24));
        service = new BookingService(bookingRepository, eventRepository, seatRepository, userRepository,
                paymentGateway, referenceGenerator, properties, Clock.fixed(NOW, ZoneOffset.UTC));

        venue = new Venue("Hall", "Oslo", "Street 1", 3, 4, NOW);
        event = eventStartingAt(NOW.plus(Duration.ofDays(5)));
        seats = SeatLayout.generate(event, venue);
    }

    @Test
    void rejectsMoreSeatsThanAllowed() {
        HoldSeatsRequest request = new HoldSeatsRequest(EVENT_ID, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L));

        assertThatThrownBy(() -> service.holdSeats(USER_ID, request))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("TOO_MANY_SEATS"));
        verifyNoInteractions(eventRepository, seatRepository, bookingRepository);
    }

    @Test
    void rejectsEventsThatAlreadyStarted() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventStartingAt(NOW.minusSeconds(60))));

        assertThatThrownBy(() -> service.holdSeats(USER_ID, new HoldSeatsRequest(EVENT_ID, List.of(1L))))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("EVENT_CLOSED"));
    }

    @Test
    void rejectsSeatsThatDoNotBelongToTheEvent() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(seatRepository.findByEventIdAndIdIn(any(), any())).thenReturn(List.of(seat("A1")));

        assertThatThrownBy(() -> service.holdSeats(USER_ID, new HoldSeatsRequest(EVENT_ID, List.of(1L, 999L))))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("INVALID_SEATS"));
    }

    @Test
    void tellsTheUserWhichSeatsAreAlreadyTaken() {
        seat("A1").hold(); // someone else is paying for A1
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(seatRepository.findByEventIdAndIdIn(any(), any())).thenReturn(List.of(seat("A1"), seat("A2")));

        assertThatThrownBy(() -> service.holdSeats(USER_ID, new HoldSeatsRequest(EVENT_ID, List.of(1L, 2L))))
                .isInstanceOfSatisfying(SeatsUnavailableException.class,
                        ex -> assertThat(ex.getSeatLabels()).containsExactly("A1"));
    }

    @Test
    void holdsAvailableSeatsForTenMinutes() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(seatRepository.findByEventIdAndIdIn(any(), any())).thenReturn(List.of(seat("C3"), seat("A1")));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user());
        when(referenceGenerator.next()).thenReturn("PLS-TEST01");
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = service.holdSeats(USER_ID, new HoldSeatsRequest(EVENT_ID, List.of(1L, 2L)));

        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.reference()).isEqualTo("PLS-TEST01");
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        assertThat(response.seats()).extracting(BookingResponse.SeatInfo::row).containsExactly("A", "C");
    }

    @Test
    void aLostRaceIsReportedAsSeatsUnavailable() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(seatRepository.findByEventIdAndIdIn(any(), any())).thenReturn(List.of(seat("B1")));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user());
        when(referenceGenerator.next()).thenReturn("PLS-TEST02");
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Seat.class, 1L));

        assertThatThrownBy(() -> service.holdSeats(USER_ID, new HoldSeatsRequest(EVENT_ID, List.of(1L))))
                .isInstanceOf(SeatsUnavailableException.class);
    }

    @Test
    void cannotPayAfterTheHoldExpired() {
        Booking booking = Booking.hold("PLS-TEST03", user(), event, List.of(seat("B2")),
                NOW.minus(Duration.ofMinutes(11)), Duration.ofMinutes(10));
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.pay(USER_ID, 1L))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("HOLD_EXPIRED"));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void cannotSeeOtherUsersBookings() {
        Booking booking = Booking.hold("PLS-TEST04", user(), event, List.of(seat("B3")), NOW, Duration.ofMinutes(10));
        when(bookingRepository.findWithEventById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.findMyBooking(999L, 1L))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("NOT_FOUND"));
    }

    @Test
    void payingConfirmsTheBooking() {
        Booking booking = Booking.hold("PLS-TEST05", user(), event, List.of(seat("A4")), NOW.minusSeconds(30),
                Duration.ofMinutes(10));
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(paymentGateway.charge(anyString(), eq(new BigDecimal("300.00"))))
                .thenReturn(PaymentResult.success("SIM-1"));

        BookingResponse response = service.pay(USER_ID, 1L);

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.cancellable()).isTrue();
    }

    private Event eventStartingAt(Instant startsAt) {
        return new Event("Concert", "Description", EventCategory.CONCERT, venue, startsAt,
                new BigDecimal("300.00"), new BigDecimal("200.00"), new BigDecimal("100.00"), NOW);
    }

    private User user() {
        User user = new User("deniz@test.dev", "hash", "Deniz", Role.USER, NOW);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private Seat seat(String label) {
        return seats.stream().filter(seat -> seat.label().equals(label)).findFirst().orElseThrow();
    }
}
