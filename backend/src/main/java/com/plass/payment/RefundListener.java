package com.plass.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends refunds to the payment provider once the cancellation is saved.
 * <p>
 * Refunding inside the transaction was wrong: if the commit failed afterwards, the booking stayed
 * paid but the money had already gone back. Running after commit means a rolled-back cancellation
 * never refunds anything.
 * <p>
 * The opposite risk remains: the cancellation is saved but the refund call fails. That is logged
 * for manual follow-up here; a production system would store the request in an outbox table and
 * retry it until the provider confirms.
 */
@Component
public class RefundListener {

    private static final Logger log = LoggerFactory.getLogger(RefundListener.class);

    private final PaymentGateway paymentGateway;

    public RefundListener(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refund(RefundRequested request) {
        try {
            paymentGateway.refund(request.transactionId(), request.amount());
        }
        catch (RuntimeException ex) {
            // The cancellation is already committed, so don't fail the user's request: flag it instead.
            log.error("Refund of {} for booking {} (transaction {}) failed and needs manual follow-up",
                    request.amount(), request.bookingReference(), request.transactionId(), ex);
        }
    }
}
