package com.sisibibi.api.domain.payment.service;

import com.sisibibi.api.domain.payment.client.PaymentApproval;
import com.sisibibi.api.domain.payment.dto.response.PaymentRes;
import com.sisibibi.api.domain.payment.entity.CustomAiReportRequest;
import com.sisibibi.api.domain.payment.entity.Payment;
import com.sisibibi.api.domain.payment.entity.PaymentStatus;
import com.sisibibi.api.domain.payment.outbox.CustomAiReportGenerationOutboxWriter;
import com.sisibibi.api.domain.payment.repository.CustomAiReportRequestRepository;
import com.sisibibi.api.domain.payment.repository.PaymentRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentCompletionService {

  private final PaymentRepository paymentRepository;
  private final CustomAiReportRequestRepository customAiReportRequestRepository;
  private final CustomAiReportGenerationOutboxWriter outboxWriter;

  @Transactional
  public PaymentRes completePayment(Long userId, PaymentApproval approval) {
    Payment payment = paymentRepository.findByOrderIdAndUserId(approval.orderId(), userId)
        .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

    validatePaymentKeyNotUsedByAnotherPayment(payment, approval.paymentKey());

    return completeLockedPayment(payment, approval.paymentKey(), approval.amount());
  }
  @Transactional
  public PaymentRes completePaymentByWebhook(String orderId, String paymentKey, long amount) {
    Payment payment = paymentRepository.findByOrderId(orderId)
        .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

    validatePaymentKeyNotUsedByAnotherPayment(payment, paymentKey);

    return completeLockedPayment(payment, paymentKey, amount);
  }

  private void validatePaymentKeyNotUsedByAnotherPayment(Payment currentPayment, String paymentKey) {
    paymentRepository.findByPaymentKey(paymentKey)
        .ifPresent(existing -> {
          if (!existing.getId().equals(currentPayment.getId())) {
            throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);
          }
        });
  }

  private PaymentRes completeLockedPayment(Payment payment, String paymentKey, long amount) {
    if (payment.getStatus() == PaymentStatus.COMPLETED) {
      return PaymentRes.from(payment);
    }

    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new CustomException(ErrorCode.PAYMENT_FAILED);
    }

    if (payment.getAmount() != amount) {
      payment.fail(ErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());
      throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    payment.complete(paymentKey);

    CustomAiReportRequest customRequest = customAiReportRequestRepository
        .findById(payment.getTargetId())
        .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));

    customRequest.markPaid();
    outboxWriter.record(customRequest.getId(), LocalDateTime.now());

    return PaymentRes.from(payment);
  }
}