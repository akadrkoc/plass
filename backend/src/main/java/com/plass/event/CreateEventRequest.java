package com.plass.event;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull EventCategory category,
        @NotNull Long venueId,
        @NotNull @Future Instant startsAt,
        @NotNull @DecimalMin("1.00") @Digits(integer = 8, fraction = 2) BigDecimal vipPrice,
        @NotNull @DecimalMin("1.00") @Digits(integer = 8, fraction = 2) BigDecimal premiumPrice,
        @NotNull @DecimalMin("1.00") @Digits(integer = 8, fraction = 2) BigDecimal standardPrice) {
}
