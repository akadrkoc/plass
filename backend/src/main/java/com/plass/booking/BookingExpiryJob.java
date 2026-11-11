package com.plass.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically frees seats whose hold expired without payment.
 * (With several app instances you would add a distributed lock such as ShedLock.)
 */
@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingService bookingService;

    public BookingExpiryJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${app.booking.expiry-check-interval-ms:30000}",
            initialDelayString = "${app.booking.expiry-check-interval-ms:30000}")
    public void releaseExpiredHolds() {
        int expired = bookingService.expireOverdueHolds();
        if (expired > 0) {
            log.info("Released seats of {} expired booking(s)", expired);
        }
    }
}
