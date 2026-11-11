package com.plass.event;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * One sellable seat of one event.
 * <p>
 * {@link Version} enables optimistic locking: Hibernate adds {@code WHERE version = ?} to every
 * UPDATE. If two requests try to hold the same seat at the same time, only the first UPDATE
 * matches; the second one fails with an optimistic locking exception and is reported as a 409.
 */
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "row_label", nullable = false, length = 3)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    @Version
    private Long version;

    protected Seat() {
        // for JPA
    }

    public Seat(Event event, String rowLabel, int seatNumber, SeatTier tier) {
        this.event = event;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.tier = tier;
        this.status = SeatStatus.AVAILABLE;
    }

    /** Human-friendly label such as "C7". */
    public String label() {
        return rowLabel + seatNumber;
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public void hold() {
        requireStatus(SeatStatus.AVAILABLE);
        this.status = SeatStatus.HELD;
    }

    public void markBooked() {
        requireStatus(SeatStatus.HELD);
        this.status = SeatStatus.BOOKED;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
    }

    private void requireStatus(SeatStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Seat " + label() + " is " + status + ", expected " + expected);
        }
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public SeatTier getTier() {
        return tier;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }
}
