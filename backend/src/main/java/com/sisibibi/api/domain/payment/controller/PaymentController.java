package com.sisibibi.api.domain.payment.controller;

import com.sisibibi.api.domain.payment.dto.request.ConfirmPaymentReq;
import com.sisibibi.api.domain.payment.dto.request.CreateCustomAiReportPaymentReq;
import com.sisibibi.api.domain.payment.dto.request.RecoverPaymentReq;
import com.sisibibi.api.domain.payment.dto.response.PaymentRes;
import com.sisibibi.api.domain.payment.service.PaymentService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/custom-ai-report")
  public ResponseEntity<ApiResponse<PaymentRes>> createCustomAiReportPayment(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody CreateCustomAiReportPaymentReq request
  ) {
    if (principal == null) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    return ResponseEntity.ok(ApiResponse.ok(
        "결제 요청이 생성되었습니다.",
        paymentService.createCustomAiReportPayment(principal.userId(), request)
    ));
  }

  @PostMapping("/confirm")
  public ResponseEntity<ApiResponse<PaymentRes>> confirmPayment(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody ConfirmPaymentReq request
  ) {
    if (principal == null) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    return ResponseEntity.ok(ApiResponse.ok(
        "결제가 승인되었습니다.",
        paymentService.confirmPayment(principal.userId(), request)
    ));
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<List<PaymentRes>>> getMyPayments(
      @AuthenticationPrincipal AuthPrincipal principal
  ) {
    if (principal == null) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    return ResponseEntity.ok(ApiResponse.ok(
        "결제 내역 조회가 완료되었습니다.",
        paymentService.getMyPayments(principal.userId())
    ));
  }

  @GetMapping("/{paymentId}")
  public ResponseEntity<ApiResponse<PaymentRes>> getPayment(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable @Positive Long paymentId
  ) {
    if (principal == null) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    return ResponseEntity.ok(ApiResponse.ok(
        "결제 상세 조회가 완료되었습니다.",
        paymentService.getPayment(principal.userId(), paymentId)
    ));
  }

  @PostMapping("/recover")
  public ResponseEntity<ApiResponse<PaymentRes>> recoverPayment(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody RecoverPaymentReq request
  ) {
    if (principal == null) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    return ResponseEntity.ok(ApiResponse.ok(
        "결제 복구가 완료되었습니다.",
        paymentService.recoverApprovedPayment(principal.userId(), request)
    ));
  }
}