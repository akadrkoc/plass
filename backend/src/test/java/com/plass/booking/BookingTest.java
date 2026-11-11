package com.plass.booking;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.plass.event.Event;
import com.plass.event.EventCategory;
import com.plass.event.Seat;
import com.plass.event.SeatLayout;
import com.plass.event.SeatStatus;
import com.plass.user.Role;
import com.plass.user.User;
import com.plass.venue.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests of the booking state machine; no Spring, no database. */
class BookingTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private static final Duration HOLD = Duration.ofMinutes(10);

    private User user;
    private Event event;
    private List<Seat> seats; // A = VIP 300, B = PREMIUM 200, C = STANDARD 100

    @BeforeEach
    void setUp() {
        user = new User("deniz@test.dev", "hash", "Deniz", Role.USER, NOW);
        Venue venue = new Venue("Hall", "İzmir", "Street 1", 3, 4, NOW);
        event = new Event("Concert", "Description", EventCategory.CONCERT, venue, NOW.plus(Duration.ofDays(3)),
                new BigDecimal("300.00"), new BigDecimal("200.00"), new BigDecimal("100.00"), NOW);
        seats = SeatLayout.generate(event, venue);
    }

    @Test
    void holdingSeatsMarksThemHeldAndSumsTierPrices() {
        Booking booking = Booking.hold("PLS-AAAAAA", user, event, List.of(seat("A1"), seat("C4")), NOW, HOLD);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(booking.getExpiresAt()).isEqualTo(NOW.plus(HOLD));
        assertThat(seat("A1").getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(seat("C4").getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    void paymentConfirmsBookingAndBooksSeats() {
        Booking booking = Booking.hold("PLS-AAAAAA", user, event, List.of(seat("B2")), NOW, HOLD);

        booking.confirm("PAY-1", NOW.plus(Duration.ofMinutes(5)));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentReference()).isEqualTo("PAY-1");
        assertThat(seat("B2").getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void cannotConfirmAfterTheHoldExpired() {
        Booking booking = Booking.hold("PLS-AAAAAA", user, event, List.of(seat("B2")), NOW, HOLD);

        assertThatThrownBy(() -> booking.confirm("PAY-1", NOW.plus(HOLD)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expiringReleasesTheSeats() {
        Booking booking = Booking.hold("PLS-AAAAAA", user, event, List.of(seat("A1"), seat("A2")), NOW, HOLD);

        booking.expire();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(seat("A1").isAvailable()).isTrue();
        assertThat(seat("A2").isAvailable()).isTrue();
    }

    @Test
    void cancellingAPaidBookingReleasesTheSeats() {
        Booking booking = Booking.hold("PLS-AAAAAA", user, event, List.of(seat("C1")), NOW, HOLD);
        booking.confirm("PAY-1", NOW.plusSeconds(60));

        booking.cancel(NOW.plus(Duration.ofDays(1)));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isEqualTo(NOW.plus(Duration.ofDays(1)));
        assertThat(seat("C1").isAvailable()).isTrue();
    }

    @Test
    void aSeatCannotBeHeldTwice() {
        Booking.hold("PLS-AAAAAA", user, event, List.of(seat("A1")), NOW, HOLD);

        assertThatThrownBy(() -> Booking.hold("PLS-BBBBBB", user, event, List.of(seat("A1")), NOW, HOLD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("A1");
    }

    private Seat seat(String label) {
        return seats.stream().filter(seat -> seat.label().equals(label)).findFirst().orElseThrow();
    }
}
