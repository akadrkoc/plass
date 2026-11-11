package com.plass.booking;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.plass.event.Event;
import com.plass.event.Seat;
import com.plass.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A booking is the aggregate that moves seats through their life cycle.
 * All state changes go through its methods, so the rules live in one place
 * (rich domain model) instead of being scattered across services.
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_reference", length = 64)
    private String paymentReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** Protects against e.g. "user pays" and "hold expires" happening at the same moment. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<BookingItem> items = new ArrayList<>();

    protected Booking() {
        // for JPA
    }

    /** Holds the given seats for {@code holdFor}; the user must pay before {@link #getExpiresAt()}. */
    public static Booking hold(String reference, User user, Event event, List<Seat> seats, Instant now,
            Duration holdFor) {
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("A booking needs at least one seat");
        }
        Booking booking = new Booking();
        booking.reference = reference;
        booking.user = user;
        booking.event = event;
        booking.status = BookingStatus.PENDING;
        booking.createdAt = now;
        booking.expiresAt = now.plus(holdFor);

        BigDecimal total = BigDecimal.ZERO;
        for (Seat seat : seats) {
            seat.hold();
            BigDecimal price = event.priceFor(seat.getTier());
            booking.items.add(new BookingItem(booking, seat, price));
            total = total.add(price);
        }
        booking.totalAmount = total;
        return booking;
    }

    public boolean isExpired(Instant now) {
        return status == BookingStatus.PENDING && !now.isBefore(expiresAt);
    }

    public void confirm(String paymentReference, Instant now) {
        requireStatus(BookingStatus.PENDING);
        if (isExpired(now)) {
            throw new IllegalStateException("Booking " + reference + " has expired");
        }
        items.forEach(item -> item.getSeat().markBooked());
        this.status = BookingStatus.CONFIRMED;
        this.paymentReference = paymentReference;
        this.confirmedAt = now;
    }

    public void cancel(Instant now) {
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking " + reference + " is " + status + " and cannot be cancelled");
        }
        releaseSeats();
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = now;
    }

    public void expire() {
        requireStatus(BookingStatus.PENDING);
        releaseSeats();
        this.status = BookingStatus.EXPIRED;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    private void releaseSeats() {
        items.forEach(item -> item.getSeat().release());
    }

    private void requireStatus(BookingStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Booking " + reference + " is " + status + ", expected " + expected);
        }
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public User getUser() {
        return user;
    }

    public Event getEvent() {
        return event;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public List<BookingItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
