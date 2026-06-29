package com.sisibibi.api.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ConfirmPaymentReq(
    @NotBlank String orderId,
    @NotBlank String paymentKey,
    @Positive long amount
) {
}