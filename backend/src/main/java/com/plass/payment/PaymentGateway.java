package com.plass.payment;

import java.math.BigDecimal;

/**
 * Port to an external payment provider. The app only depends on this interface, so a real
 * provider (iyzico, Stripe, ...) can replace the simulated one without touching booking logic.
 */
public interface PaymentGateway {

    PaymentResult charge(String orderReference, BigDecimal amount);

    void refund(String transactionId, BigDecimal amount);
}
