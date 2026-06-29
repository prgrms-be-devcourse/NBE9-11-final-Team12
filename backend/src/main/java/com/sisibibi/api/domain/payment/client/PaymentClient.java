package com.sisibibi.api.domain.payment.client;

public interface PaymentClient {

  PaymentApproval confirm(String paymentKey, String orderId, long amount);
}