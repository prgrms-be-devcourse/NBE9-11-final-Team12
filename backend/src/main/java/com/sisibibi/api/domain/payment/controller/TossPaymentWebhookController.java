package com.sisibibi.api.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.payment.client.TossWebhookVerifier;
import com.sisibibi.api.domain.payment.dto.request.TossPaymentWebhookReq;
import com.sisibibi.api.domain.payment.dto.response.PaymentRes;
import com.sisibibi.api.domain.payment.service.PaymentCompletionService;
import com.sisibibi.api.domain.payment.service.PaymentService;
import com.sisibibi.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments/toss/webhook")
public class TossPaymentWebhookController {

  private final ObjectMapper objectMapper;
  private final TossWebhookVerifier tossWebhookVerifier;
  private final PaymentCompletionService paymentService;

  @PostMapping
  public ResponseEntity<ApiResponse<PaymentRes>> handleWebhook(
      @RequestHeader(name = "TossPayments-Signature", required = false) String signature,
      @RequestBody String rawBody
  ) {
    tossWebhookVerifier.verify(rawBody, signature);

    TossPaymentWebhookReq request = TossPaymentWebhookReq.from(rawBody, objectMapper);

    if (!request.isPaymentApproved()) {
      return ResponseEntity.ok(ApiResponse.ok("처리 대상이 아닌 결제 웹훅입니다.", null));
    }

    PaymentRes response = paymentService.completePaymentByWebhook(
        request.orderId(),
        request.paymentKey(),
        request.amount()
    );

    return ResponseEntity.ok(ApiResponse.ok("결제 웹훅 처리가 완료되었습니다.", response));
  }
}