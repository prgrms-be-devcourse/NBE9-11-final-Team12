package com.sisibibi.api.domain.payment.service;

import com.sisibibi.api.domain.payment.client.PaymentApproval;
import com.sisibibi.api.domain.payment.client.PaymentClient;
import com.sisibibi.api.domain.payment.dto.request.ConfirmPaymentReq;
import com.sisibibi.api.domain.payment.dto.request.CreateCustomAiReportPaymentReq;
import com.sisibibi.api.domain.payment.dto.request.RecoverPaymentReq;
import com.sisibibi.api.domain.payment.dto.response.PaymentRes;
import com.sisibibi.api.domain.payment.entity.*;
import com.sisibibi.api.domain.payment.outbox.CustomAiReportGenerationOutboxWriter;
import com.sisibibi.api.domain.payment.repository.CustomAiReportRequestRepository;
import com.sisibibi.api.domain.payment.repository.PaymentRepository;
import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
import com.sisibibi.api.domain.report.entity.AiReportStatus;
import com.sisibibi.api.domain.report.prompt.CustomPromptValidator;
import com.sisibibi.api.domain.report.repository.AiReportRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final CustomAiReportRequestRepository customAiReportRequestRepository;
  private final AiReportRepository aiReportRepository;
  private final CustomPromptValidator customPromptValidator;
  private final PaymentClient paymentClient;
  private final CustomAiReportGenerationOutboxWriter outboxWriter;

  private static final long CUSTOM_AI_REPORT_PRICE = 3000L;

  @Transactional
  public PaymentRes createCustomAiReportPayment(Long userId, CreateCustomAiReportPaymentReq request) {
    validateBaseReportCompleted(request.roomId());

    if (request.amount() != customAiReportPrice()) {
      throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    List<AiReportCustomPrompt> prompts = customPromptValidator.normalizeAndScan(request.toAiReportGenerateReq())
        .stream()
        .map(prompt -> new AiReportCustomPrompt(userId, prompt.label(), prompt.prompt()))
        .toList();

    CustomAiReportRequest customRequest = customAiReportRequestRepository.save(
        CustomAiReportRequest.pendingPayment(request.roomId(), userId, prompts)
    );

    String orderId = "custom-ai-report-" + customRequest.getId() + "-" + UUID.randomUUID();
    Payment payment = paymentRepository.save(Payment.pending(
        userId,
        orderId,
        request.amount(),
        "커스텀 AI 리포트",
        PaymentTargetType.CUSTOM_AI_REPORT,
        customRequest.getId()
    ));

    customRequest.linkPayment(payment.getId());
    return PaymentRes.from(payment);
  }

  public PaymentRes confirmPayment(Long userId, ConfirmPaymentReq request) {
    validatePaymentKeyNotUsedByAnotherOrder(userId, request);

    Payment payment = paymentRepository.findByOrderIdAndUserId(request.orderId(), userId)
        .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

    if (payment.getStatus() == PaymentStatus.COMPLETED) {
      return PaymentRes.from(payment);
    }

    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new CustomException(ErrorCode.PAYMENT_FAILED);
    }

    validateAmount(payment, request.amount());

    PaymentApproval approval = paymentClient.confirm(
        request.paymentKey(),
        request.orderId(),
        request.amount()
    );

    return completePayment(userId, approval);
  }

  @Transactional
  public PaymentRes completePayment(Long userId, PaymentApproval approval) {
    Payment payment = paymentRepository.findByOrderIdAndUserId(approval.orderId(), userId)
        .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

    if (payment.getStatus() == PaymentStatus.COMPLETED) {
      return PaymentRes.from(payment);
    }

    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new CustomException(ErrorCode.PAYMENT_FAILED);
    }

    validateAmount(payment, approval.amount());
    payment.complete(approval.paymentKey());

    CustomAiReportRequest customRequest = customAiReportRequestRepository
        .findByIdAndUserId(payment.getTargetId(), userId)
        .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));

    customRequest.markPaid();
    outboxWriter.record(customRequest.getId(), LocalDateTime.now());

    return PaymentRes.from(payment);
  }

  @Transactional(readOnly = true)
  public List<PaymentRes> getMyPayments(Long userId) {
    return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
        .stream()
        .map(PaymentRes::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public PaymentRes getPayment(Long userId, Long paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .filter(found -> found.getUserId().equals(userId))
        .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

    return PaymentRes.from(payment);
  }

  private void validateBaseReportCompleted(Long roomId) {
    boolean completed = aiReportRepository.findByRoomId(roomId)
        .filter(report -> report.getStatus() == AiReportStatus.COMPLETED)
        .isPresent();

    if (!completed) {
      throw new CustomException(ErrorCode.AI_REPORT_NOT_FOUND);
    }
  }

  private void validatePaymentKeyNotUsedByAnotherOrder(Long userId, ConfirmPaymentReq request) {
    paymentRepository.findByPaymentKey(request.paymentKey())
        .ifPresent(existing -> {
          boolean samePayment = existing.getUserId().equals(userId)
              && existing.getOrderId().equals(request.orderId())
              && existing.getAmount() == request.amount();

          if (!samePayment) {
            throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);
          }
        });
  }

  @Transactional
  public PaymentRes recoverApprovedPayment(Long userId, RecoverPaymentReq request) {
    validatePaymentKeyNotUsedByAnotherOrder(
        userId,
        new ConfirmPaymentReq(request.orderId(), request.paymentKey(), request.amount())
    );

    return completePayment(
        userId,
        new PaymentApproval(request.paymentKey(), request.orderId(), request.amount())
    );
  }

  private void validateAmount(Payment payment, long amount) {
    if (payment.getAmount() != amount) {
      payment.fail(ErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());
      throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }
  }

  private long customAiReportPrice() {
    return CUSTOM_AI_REPORT_PRICE;
  }
}