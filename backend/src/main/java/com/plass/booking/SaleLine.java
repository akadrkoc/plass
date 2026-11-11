package com.plass.booking;

import java.math.BigDecimal;
import java.time.Instant;

/** One sold ticket: when it was paid and for how much. Used for sales charts. */
public record SaleLine(Instant confirmedAt, BigDecimal price) {
}
