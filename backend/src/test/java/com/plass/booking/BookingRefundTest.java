package com.plass.booking;

import java.util.List;

import com.plass.event.Event;
import com.plass.payment.PaymentGateway;
import com.plass.support.IntegrationTest;
import com.plass.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Money must only go back to the customer once the cancellation is really saved. */
class BookingRefundTest extends IntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private PaymentGateway paymentGateway;

    @Test
    void cancellingPaidTicketsRefundsThem() {
        User user = data.user();
        BookingResponse paid = paidBooking(user, data.upcomingEvent());

        bookingService.cancel(user.getId(), paid.id());

        verify(paymentGateway, times(1)).refund(any(), eq(paid.totalAmount()));
    }

    @Test
    void noRefundWhenTheCancellationIsRolledBack() {
        User user = data.user();
        BookingResponse paid = paidBooking(user, data.upcomingEvent());

        // Something fails after the booking was cancelled but before the transaction commits.
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            bookingService.cancel(user.getId(), paid.id());
            throw new IllegalStateException("simulated failure before commit");
        })).isInstanceOf(IllegalStateException.class);

        verify(paymentGateway, never()).refund(any(), any());
        assertThat(bookingService.findMyBooking(user.getId(), paid.id()).status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    private BookingResponse paidBooking(User user, Event event) {
        List<Long> seats = data.seatIds(event, "A1", "A2");
        BookingResponse held = bookingService.holdSeats(user.getId(), new HoldSeatsRequest(event.getId(), seats));
        return bookingService.pay(user.getId(), held.id());
    }
}
