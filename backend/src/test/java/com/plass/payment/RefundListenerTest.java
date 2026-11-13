package com.plass.payment;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefundListenerTest {

    private static final RefundRequested REQUEST =
            new RefundRequested("PLS-TEST01", "SIM-123", new BigDecimal("600.00"));

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private RefundListener listener;

    @Test
    void sendsTheRefundToThePaymentProvider() {
        listener.refund(REQUEST);

        verify(paymentGateway).refund("SIM-123", new BigDecimal("600.00"));
    }

    @Test
    void aFailedRefundDoesNotFailTheAlreadySavedCancellation() {
        doThrow(new IllegalStateException("provider down")).when(paymentGateway).refund("SIM-123", REQUEST.amount());

        assertThatCode(() -> listener.refund(REQUEST)).doesNotThrowAnyException();
    }
}
