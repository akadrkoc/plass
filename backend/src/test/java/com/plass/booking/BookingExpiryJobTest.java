package com.plass.booking;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests of the expiry job (fast, no Docker needed). */
@ExtendWith(MockitoExtension.class)
class BookingExpiryJobTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingExpiryJob job;

    @Test
    void oneFailingBookingDoesNotStopTheOthers() {
        when(bookingService.findOverdueHoldIds(BookingExpiryJob.BATCH_SIZE)).thenReturn(List.of(1L, 2L, 3L));
        when(bookingService.expireHold(1L)).thenReturn(true);
        when(bookingService.expireHold(2L))
                .thenThrow(new ObjectOptimisticLockingFailureException(Booking.class, 2L));
        when(bookingService.expireHold(3L)).thenReturn(true);

        assertThat(job.expireOverdueHolds()).isEqualTo(2);
        verify(bookingService).expireHold(3L);
    }

    @Test
    void bookingsThatWerePaidInTheMeantimeAreNotCounted() {
        when(bookingService.findOverdueHoldIds(BookingExpiryJob.BATCH_SIZE)).thenReturn(List.of(1L, 2L));
        when(bookingService.expireHold(1L)).thenReturn(false);
        when(bookingService.expireHold(2L)).thenReturn(true);

        assertThat(job.expireOverdueHolds()).isEqualTo(1);
    }
}
