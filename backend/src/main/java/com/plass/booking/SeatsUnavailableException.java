package com.plass.booking;

import java.util.List;

import com.plass.common.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when at least one requested seat is already held or booked by someone else. */
public class SeatsUnavailableException extends ApiException {

    private final List<String> seatLabels;

    public SeatsUnavailableException(List<String> seatLabels) {
        super(HttpStatus.CONFLICT, "SEATS_UNAVAILABLE",
                "Sorry, these seats were just taken: " + String.join(", ", seatLabels));
        this.seatLabels = List.copyOf(seatLabels);
    }

    public List<String> getSeatLabels() {
        return seatLabels;
    }
}
