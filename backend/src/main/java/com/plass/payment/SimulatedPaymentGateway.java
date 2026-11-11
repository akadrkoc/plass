package com.plass.payment;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Demo implementation: every payment succeeds and no money moves. */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    @Override
    public PaymentResult charge(String orderReference, BigDecimal amount) {
        String transactionId = "SIM-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT);
        log.info("Simulated charge of {} NOK for {} -> {}", amount, orderReference, transactionId);
        return PaymentResult.success(transactionId);
    }

    @Override
    public void refund(String transactionId, BigDecimal amount) {
        log.info("Simulated refund of {} NOK for transaction {}", amount, transactionId);
    }
}
