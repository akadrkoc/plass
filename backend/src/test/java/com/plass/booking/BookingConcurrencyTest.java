package com.plass.booking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.plass.common.ApiException;
import com.plass.event.Event;
import com.plass.event.SeatStatus;
import com.plass.payment.PaymentGateway;
import com.plass.support.IntegrationTest;
import com.plass.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The key guarantees of the system: a seat can never be sold twice, even when many people
 * click "Reserve" on the same seat at exactly the same moment, and a booking is never charged twice,
 * even when "Pay" is clicked several times at once.
 */
class BookingConcurrencyTest extends IntegrationTest {

    private static final int THREADS = 20;

    @Autowired
    private BookingService bookingService;

    @MockitoSpyBean
    private PaymentGateway paymentGateway;

    @Test
    void onlyOneOfManySimultaneousRequestsGetsTheSeat() throws Exception {
        Event event = data.upcomingEvent();
        List<Long> seat = data.seatIds(event, "A1");
        List<User> users = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            users.add(data.user());
        }

        List<Boolean> results = runAtTheSameTime(users.stream()
                .map(user -> (Callable<Boolean>) () -> tryToHold(user, event, seat))
                .toList());

        assertThat(results).filteredOn(success -> success).hasSize(1);
        assertThat(data.seatsOf(event)).filteredOn(s -> s.label().equals("A1"))
                .singleElement().extracting(s -> s.getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    void differentSeatsCanBeBookedInParallel() throws Exception {
        Event event = data.upcomingEvent();
        List<String> labels = List.of("A1", "A2", "B1", "B2", "C1", "C2");

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (String label : labels) {
            User user = data.user();
            List<Long> seat = data.seatIds(event, label);
            tasks.add(() -> tryToHold(user, event, seat));
        }

        assertThat(runAtTheSameTime(tasks)).containsOnly(true);
    }

    @Test
    void simultaneousPayClicksChargeTheCustomerOnlyOnce() throws Exception {
        Event event = data.upcomingEvent();
        User user = data.user();
        BookingResponse booking = bookingService.holdSeats(user.getId(),
                new HoldSeatsRequest(event.getId(), data.seatIds(event, "A1", "A2")));

        List<Callable<String>> clicks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            clicks.add(() -> tryToPay(user, booking.id()));
        }

        assertThat(runAtTheSameTime(clicks))
                .containsOnlyOnce("PAID")
                .filteredOn(result -> !result.equals("PAID"))
                .containsOnly("BOOKING_NOT_PENDING");
        verify(paymentGateway, times(1)).charge(eq(booking.reference()), any());
    }

    private String tryToPay(User user, Long bookingId) {
        try {
            bookingService.pay(user.getId(), bookingId);
            return "PAID";
        }
        catch (ApiException ex) {
            return ex.getCode();
        }
    }

    private boolean tryToHold(User user, Event event, List<Long> seatIds) {
        try {
            bookingService.holdSeats(user.getId(), new HoldSeatsRequest(event.getId(), seatIds));
            return true;
        }
        catch (SeatsUnavailableException ex) {
            return false;
        }
    }

    /** Starts all tasks together using a latch, so they really race for the same rows. */
    private static <T> List<T> runAtTheSameTime(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch startSignal = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    startSignal.await();
                    return task.call();
                }));
            }
            startSignal.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS)); // any unexpected exception fails the test
            }
            return results;
        }
        finally {
            pool.shutdownNow();
        }
    }
}
