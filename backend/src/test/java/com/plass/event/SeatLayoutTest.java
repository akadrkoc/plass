package com.plass.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.plass.venue.Venue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeatLayoutTest {

    @Test
    void frontRowsAreVipThenPremiumThenStandard() {
        assertThat(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).stream().map(row -> SeatLayout.tierForRow(row, 10)))
                .containsExactly(
                        SeatTier.VIP, SeatTier.VIP,
                        SeatTier.PREMIUM, SeatTier.PREMIUM, SeatTier.PREMIUM,
                        SeatTier.STANDARD, SeatTier.STANDARD, SeatTier.STANDARD, SeatTier.STANDARD, SeatTier.STANDARD);
    }

    @Test
    void smallVenuesStillHaveAtLeastOneVipRow() {
        assertThat(SeatLayout.tierForRow(0, 1)).isEqualTo(SeatTier.VIP);
        assertThat(SeatLayout.tierForRow(0, 3)).isEqualTo(SeatTier.VIP);
        assertThat(SeatLayout.tierForRow(1, 3)).isEqualTo(SeatTier.PREMIUM);
        assertThat(SeatLayout.tierForRow(2, 3)).isEqualTo(SeatTier.STANDARD);
    }

    @Test
    void generatesOneAvailableSeatPerPosition() {
        Venue venue = new Venue("Hall", "İstanbul", "Street 1", 3, 4, Instant.now());
        Event event = new Event("Show", "Description", EventCategory.THEATRE, venue, Instant.now().plusSeconds(3600),
                new BigDecimal("300"), new BigDecimal("200"), new BigDecimal("100"), Instant.now());

        List<Seat> seats = SeatLayout.generate(event, venue);

        assertThat(seats).hasSize(12);
        assertThat(seats).extracting(Seat::label)
                .containsExactly("A1", "A2", "A3", "A4", "B1", "B2", "B3", "B4", "C1", "C2", "C3", "C4");
        assertThat(seats).allMatch(Seat::isAvailable);
    }
}
