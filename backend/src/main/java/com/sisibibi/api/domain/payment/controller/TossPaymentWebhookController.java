package com.sisibibi.api.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.payment.client.PaymentApproval;
import com.sisibibi.api.domain.payment.client.PaymentClient;
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
  private final PaymentClient paymentClient;
  private final PaymentCompletionService paymentCompletionService;

  @PostMapping
  public ResponseEntity<ApiResponse<PaymentRes>> handleWebhook(@RequestBody String rawBody) {
    TossPaymentWebhookReq request = TossPaymentWebhookReq.from(rawBody, objectMapper);

    if (!request.isPaymentStatusChanged() || !request.isPaymentApproved()) {
      return ResponseEntity.ok(ApiResponse.ok("처리 대상이 아닌 결제 웹훅입니다.", null));
    }

    PaymentApproval approval = paymentClient.getPayment(request.paymentKey());
    if (!approval.isDone()
        || !approval.orderId().equals(request.orderId())
        || approval.amount() != request.amount()) {
      return ResponseEntity.ok(ApiResponse.ok("검증되지 않은 결제 웹훅입니다.", null));
    }

    PaymentRes response = paymentCompletionService.completePaymentByWebhook(
        approval.orderId(),
        approval.paymentKey(),
        approval.amount()
    );

    return ResponseEntity.ok(ApiResponse.ok("결제 웹훅 처리가 완료되었습니다.", response));
  }
}