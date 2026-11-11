package com.plass.venue;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A physical place with a simple rectangular seating plan (rows A, B, C... × seats per row). */
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(nullable = false)
    private String address;

    @Column(name = "seat_rows", nullable = false)
    private int rows;

    @Column(name = "seats_per_row", nullable = false)
    private int seatsPerRow;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Venue() {
        // for JPA
    }

    public Venue(String name, String city, String address, int rows, int seatsPerRow, Instant createdAt) {
        this.name = name;
        this.city = city;
        this.address = address;
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
        this.createdAt = createdAt;
    }

    public int capacity() {
        return rows * seatsPerRow;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public int getRows() {
        return rows;
    }

    public int getSeatsPerRow() {
        return seatsPerRow;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
