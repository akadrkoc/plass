package com.plass.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import com.plass.venue.Venue;
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

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "vip_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal vipPrice;

    @Column(name = "premium_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumPrice;

    @Column(name = "standard_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal standardPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Event() {
        // for JPA
    }

    public Event(String title, String description, EventCategory category, Venue venue, Instant startsAt,
            BigDecimal vipPrice, BigDecimal premiumPrice, BigDecimal standardPrice, Instant createdAt) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.venue = venue;
        this.startsAt = startsAt;
        this.vipPrice = vipPrice;
        this.premiumPrice = premiumPrice;
        this.standardPrice = standardPrice;
        this.createdAt = createdAt;
        this.status = EventStatus.PUBLISHED;
    }

    public BigDecimal priceFor(SeatTier tier) {
        return switch (tier) {
            case VIP -> vipPrice;
            case PREMIUM -> premiumPrice;
            case STANDARD -> standardPrice;
        };
    }

    public BigDecimal lowestPrice() {
        return Stream.of(vipPrice, premiumPrice, standardPrice).min(BigDecimal::compareTo).orElseThrow();
    }

    /** Tickets can be bought only for published events that haven't started yet. */
    public boolean isOpenForBooking(Instant now) {
        return status == EventStatus.PUBLISHED && now.isBefore(startsAt);
    }

    public void cancel() {
        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException("Event " + id + " is already cancelled");
        }
        this.status = EventStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Venue getVenue() {
        return venue;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public BigDecimal getVipPrice() {
        return vipPrice;
    }

    public BigDecimal getPremiumPrice() {
        return premiumPrice;
    }

    public BigDecimal getStandardPrice() {
        return standardPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
