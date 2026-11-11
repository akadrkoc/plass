package com.plass.booking;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HoldSeatsRequest(
        @NotNull Long eventId,
        @NotEmpty @Size(max = 20) List<@NotNull Long> seatIds) {
}
