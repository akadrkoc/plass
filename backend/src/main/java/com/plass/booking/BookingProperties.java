package com.plass.booking;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(Duration holdDuration, int maxSeatsPerBooking, Duration cancellationCutoff) {
}
