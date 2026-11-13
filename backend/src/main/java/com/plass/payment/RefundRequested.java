package com.plass.payment;

import java.math.BigDecimal;

/**
 * Published when a paid booking is cancelled. {@link RefundListener} sends the refund to the payment
 * provider only after the cancellation has been committed.
 */
public record RefundRequested(String bookingReference, String transactionId, BigDecimal amount) {
}
