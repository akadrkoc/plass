package com.plass.payment;

public record PaymentResult(boolean successful, String transactionId, String failureReason) {

    public static PaymentResult success(String transactionId) {
        return new PaymentResult(true, transactionId, null);
    }

    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
