package com.plass.booking;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import com.plass.common.ApiException;
import com.plass.event.Event;
import com.plass.event.SeatStatus;
import com.plass.support.IntegrationTest;
import com.plass.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The checkout flow against a real PostgreSQL database. */
class BookingFlowTest extends IntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Test
    void holdingAndPayingBooksTheSeats() {
        User user = data.user();
        Event event = data.upcomingEvent();

        BookingResponse held = hold(user, event, "A1", "C2");

        assertThat(held.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(held.totalAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(statusOf(event, "A1")).isEqualTo(SeatStatus.HELD);

        BookingResponse paid = bookingService.pay(user.getId(), held.id());

        assertThat(paid.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(statusOf(event, "A1")).isEqualTo(SeatStatus.BOOKED);
        assertThat(statusOf(event, "C2")).isEqualTo(SeatStatus.BOOKED);
        assertThat(bookingService.findMyBookings(user.getId())).extracting(BookingResponse::reference)
                .containsExactly(held.reference());
    }

    @Test
    void aSeatHeldBySomeoneElseCannotBeTaken() {
        Event event = data.upcomingEvent();
        hold(data.user(), event, "B1");

        assertThatThrownBy(() -> hold(data.user(), event, "B1", "B2"))
                .isInstanceOfSatisfying(SeatsUnavailableException.class,
                        ex -> assertThat(ex.getSeatLabels()).containsExactly("B1"));
        assertThat(statusOf(event, "B2")).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void aNewSelectionReplacesTheUsersPreviousHold() {
        User user = data.user();
        Event event = data.upcomingEvent();
        BookingResponse first = hold(user, event, "A1");

        hold(user, event, "A2");

        assertThat(statusOf(event, "A1")).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(statusOf(event, "A2")).isEqualTo(SeatStatus.HELD);
        assertThat(bookingService.findMyBooking(user.getId(), first.id()).status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void unpaidHoldsExpireAndReleaseTheirSeats() {
        User user = data.user();
        Event event = data.upcomingEvent();
        BookingResponse held = hold(user, event, "C3");

        clock.advance(Duration.ofMinutes(11));

        assertThatThrownBy(() -> bookingService.pay(user.getId(), held.id()))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("HOLD_EXPIRED"));

        assertThat(bookingService.expireOverdueHolds()).isGreaterThanOrEqualTo(1);
        assertThat(bookingService.findMyBooking(user.getId(), held.id()).status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(statusOf(event, "C3")).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void paidTicketsCanBeCancelledWellBeforeTheEvent() {
        User user = data.user();
        Event event = data.upcomingEvent();
        BookingResponse paid = bookingService.pay(user.getId(), hold(user, event, "B3").id());
        assertThat(paid.cancellable()).isTrue();

        BookingResponse cancelled = bookingService.cancel(user.getId(), paid.id());

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(statusOf(event, "B3")).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void paidTicketsCannotBeCancelledCloseToTheEvent() {
        User user = data.user();
        Event event = data.eventStartingIn(Duration.ofHours(12));
        BookingResponse paid = bookingService.pay(user.getId(), hold(user, event, "B4").id());
        assertThat(paid.cancellable()).isFalse();

        assertThatThrownBy(() -> bookingService.cancel(user.getId(), paid.id()))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("NOT_CANCELLABLE"));
    }

    @Test
    void usersCannotSeeEachOthersBookings() {
        Event event = data.upcomingEvent();
        BookingResponse booking = hold(data.user(), event, "C4");
        User stranger = data.user();

        assertThatThrownBy(() -> bookingService.findMyBooking(stranger.getId(), booking.id()))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("NOT_FOUND"));
    }

    private BookingResponse hold(User user, Event event, String... seats) {
        List<Long> seatIds = data.seatIds(event, seats);
        return bookingService.holdSeats(user.getId(), new HoldSeatsRequest(event.getId(), seatIds));
    }

    private SeatStatus statusOf(Event event, String label) {
        return data.seatsOf(event).stream()
                .filter(seat -> seat.label().equals(label))
                .findFirst()
                .orElseThrow()
                .getStatus();
    }
}
