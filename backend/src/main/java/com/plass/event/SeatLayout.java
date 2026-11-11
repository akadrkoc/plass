package com.plass.event;

import java.util.ArrayList;
import java.util.List;

import com.plass.venue.Venue;

/** Generates the seats of an event from its venue's rectangular plan. */
public final class SeatLayout {

    private SeatLayout() {
    }

    public static List<Seat> generate(Event event, Venue venue) {
        List<Seat> seats = new ArrayList<>(venue.capacity());
        for (int rowIndex = 0; rowIndex < venue.getRows(); rowIndex++) {
            String rowLabel = rowLabel(rowIndex);
            SeatTier tier = tierForRow(rowIndex, venue.getRows());
            for (int number = 1; number <= venue.getSeatsPerRow(); number++) {
                seats.add(new Seat(event, rowLabel, number, tier));
            }
        }
        return seats;
    }

    /** First ~20% of rows are VIP, the next ~30% PREMIUM, the rest STANDARD. */
    static SeatTier tierForRow(int rowIndex, int totalRows) {
        int vipRows = Math.max(1, Math.round(totalRows * 0.2f));
        int premiumRows = Math.round(totalRows * 0.3f);
        if (rowIndex < vipRows) {
            return SeatTier.VIP;
        }
        if (rowIndex < vipRows + premiumRows) {
            return SeatTier.PREMIUM;
        }
        return SeatTier.STANDARD;
    }

    static String rowLabel(int rowIndex) {
        return String.valueOf((char) ('A' + rowIndex));
    }
}
