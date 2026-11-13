package com.plass.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically frees seats whose hold expired without payment.
 * <p>
 * Each booking is expired in its own transaction: if one of them fails (for example because the user
 * cancelled it at the same moment), only that booking is skipped and retried on the next run, instead
 * of rolling back the whole batch.
 * (With several app instances you would add a distributed lock such as ShedLock.)
 */
@Component
public class BookingExpiryJob {

    static final int BATCH_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingService bookingService;

    public BookingExpiryJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${app.booking.expiry-check-interval-ms:30000}",
            initialDelayString = "${app.booking.expiry-check-interval-ms:30000}")
    public void releaseExpiredHolds() {
        int expired = expireOverdueHolds();
        if (expired > 0) {
            log.info("Released seats of {} expired booking(s)", expired);
        }
    }

    /** Returns how many holds were expired. */
    int expireOverdueHolds() {
        int expired = 0;
        for (Long bookingId : bookingService.findOverdueHoldIds(BATCH_SIZE)) {
            try {
                if (bookingService.expireHold(bookingId)) {
                    expired++;
                }
            }
            catch (RuntimeException ex) {
                log.warn("Could not expire booking {}, will retry on the next run", bookingId, ex);
            }
        }
        return expired;
    }
}
