package com.plass.booking;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.plass.common.ApiException;
import com.plass.event.Event;
import com.plass.event.EventRepository;
import com.plass.event.Seat;
import com.plass.event.SeatRepository;
import com.plass.payment.PaymentGateway;
import com.plass.payment.PaymentResult;
import com.plass.user.User;
import com.plass.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final BookingReferenceGenerator referenceGenerator;
    private final BookingProperties properties;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, EventRepository eventRepository,
            SeatRepository seatRepository, UserRepository userRepository, PaymentGateway paymentGateway,
            BookingReferenceGenerator referenceGenerator, BookingProperties properties, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.referenceGenerator = referenceGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Step 1 of checkout: reserve seats for a few minutes while the user pays.
     * <p>
     * Double booking is prevented by optimistic locking on {@link Seat}: if another request changed
     * one of these seats after we read it, the flush below fails and we answer with 409 Conflict.
     */
    @Transactional
    public BookingResponse holdSeats(Long userId, HoldSeatsRequest request) {
        Instant now = Instant.now(clock);
        Set<Long> seatIds = new LinkedHashSet<>(request.seatIds());
        if (seatIds.size() > properties.maxSeatsPerBooking()) {
            throw ApiException.badRequest("TOO_MANY_SEATS",
                    "You can book at most " + properties.maxSeatsPerBooking() + " seats at once");
        }

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> ApiException.notFound("Event", request.eventId()));
        if (!event.isOpenForBooking(now)) {
            throw ApiException.conflict("EVENT_CLOSED", "This event is no longer open for booking");
        }

        // A user keeps at most one active hold per event: a new selection replaces the previous one.
        bookingRepository.findByUserIdAndEventIdAndStatus(userId, event.getId(), BookingStatus.PENDING)
                .forEach(previous -> previous.cancel(now));

        List<Seat> seats = new ArrayList<>(seatRepository.findByEventIdAndIdIn(event.getId(), seatIds));
        seats.sort(Comparator.comparing(Seat::getRowLabel).thenComparingInt(Seat::getSeatNumber));
        if (seats.size() != seatIds.size()) {
            throw ApiException.badRequest("INVALID_SEATS", "Some of the selected seats do not belong to this event");
        }
        List<String> taken = seats.stream().filter(seat -> !seat.isAvailable()).map(Seat::label).sorted().toList();
        if (!taken.isEmpty()) {
            throw new SeatsUnavailableException(taken);
        }

        User user = userRepository.getReferenceById(userId);
        Booking booking = Booking.hold(newReference(), user, event, seats, now, properties.holdDuration());
        try {
            // Flush now (instead of at commit) so a lost race surfaces here as a clean 409.
            bookingRepository.saveAndFlush(booking);
        }
        catch (ObjectOptimisticLockingFailureException ex) {
            throw new SeatsUnavailableException(seats.stream().map(Seat::label).sorted().toList());
        }
        return BookingResponse.from(booking, true);
    }

    /**
     * Step 2 of checkout: charge the (simulated) payment provider and confirm the seats.
     * <p>
     * The booking row is locked first, so two simultaneous "Pay" clicks cannot both see PENDING and
     * both charge the card: the second request waits, then finds the booking CONFIRMED and gets 409.
     */
    @Transactional
    public BookingResponse pay(Long userId, Long bookingId) {
        Instant now = Instant.now(clock);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .filter(found -> found.isOwnedBy(userId))
                .orElseThrow(() -> ApiException.notFound("Booking", bookingId));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.conflict("BOOKING_NOT_PENDING", "This booking is not waiting for payment");
        }
        if (booking.isExpired(now)) {
            throw ApiException.conflict("HOLD_EXPIRED",
                    "Your seat reservation has expired. Please select your seats again.");
        }

        PaymentResult payment = paymentGateway.charge(booking.getReference(), booking.getTotalAmount());
        if (!payment.successful()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", payment.failureReason());
        }
        booking.confirm(payment.transactionId(), now);
        return BookingResponse.from(booking, isCancellable(booking, now));
    }

    @Transactional
    public BookingResponse cancel(Long userId, Long bookingId) {
        Instant now = Instant.now(clock);
        Booking booking = findOwnedBooking(userId, bookingId);
        if (!isCancellable(booking, now)) {
            throw ApiException.conflict("NOT_CANCELLABLE", booking.getStatus() == BookingStatus.CONFIRMED
                    ? "Tickets can only be cancelled until " + properties.cancellationCutoff().toHours()
                            + " hours before the event"
                    : "This booking can no longer be cancelled");
        }
        boolean wasPaid = booking.getStatus() == BookingStatus.CONFIRMED;
        booking.cancel(now);
        if (wasPaid) {
            paymentGateway.refund(booking.getPaymentReference(), booking.getTotalAmount());
        }
        return BookingResponse.from(booking, false);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findMyBookings(Long userId) {
        Instant now = Instant.now(clock);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(booking -> BookingResponse.from(booking, isCancellable(booking, now)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse findMyBooking(Long userId, Long bookingId) {
        Booking booking = findOwnedBooking(userId, bookingId);
        return BookingResponse.from(booking, isCancellable(booking, Instant.now(clock)));
    }

    /** Called by {@link BookingExpiryJob}: releases seats of holds that were not paid in time. */
    @Transactional
    public int expireOverdueHolds() {
        List<Booking> overdue = bookingRepository
                .findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(BookingStatus.PENDING, Instant.now(clock));
        overdue.forEach(Booking::expire);
        return overdue.size();
    }

    /** Used when an organiser cancels an event: every active booking is cancelled and refunded. */
    @Transactional
    public int cancelActiveBookingsForEvent(Long eventId) {
        Instant now = Instant.now(clock);
        List<Booking> active = bookingRepository.findByEventIdAndStatusIn(eventId,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        for (Booking booking : active) {
            boolean wasPaid = booking.getStatus() == BookingStatus.CONFIRMED;
            booking.cancel(now);
            if (wasPaid) {
                paymentGateway.refund(booking.getPaymentReference(), booking.getTotalAmount());
            }
        }
        return active.size();
    }

    boolean isCancellable(Booking booking, Instant now) {
        return switch (booking.getStatus()) {
            case PENDING -> !booking.isExpired(now);
            case CONFIRMED -> now.isBefore(booking.getEvent().getStartsAt().minus(properties.cancellationCutoff()));
            case CANCELLED, EXPIRED -> false;
        };
    }

    private Booking findOwnedBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findWithEventById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking", bookingId));
        if (!booking.isOwnedBy(userId)) {
            // 404 instead of 403: don't reveal that someone else's booking exists.
            throw ApiException.notFound("Booking", bookingId);
        }
        return booking;
    }

    private String newReference() {
        String reference;
        do {
            reference = referenceGenerator.next();
        }
        while (bookingRepository.existsByReference(reference));
        return reference;
    }
}
