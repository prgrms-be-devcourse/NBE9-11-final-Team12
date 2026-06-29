package com.sisibibi.api.domain.payment.service;

import com.sisibibi.api.domain.payment.client.PaymentApproval;
import com.sisibibi.api.domain.payment.dto.response.PaymentRes;
import com.sisibibi.api.domain.payment.entity.CustomAiReportRequest;
import com.sisibibi.api.domain.payment.entity.CustomAiReportRequestStatus;
import com.sisibibi.api.domain.payment.entity.Payment;
import com.sisibibi.api.domain.payment.entity.PaymentStatus;
import com.sisibibi.api.domain.payment.entity.PaymentTargetType;
import com.sisibibi.api.domain.payment.outbox.CustomAiReportGenerationOutboxWriter;
import com.sisibibi.api.domain.payment.repository.CustomAiReportRequestRepository;
import com.sisibibi.api.domain.payment.repository.PaymentRepository;
import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCompletionServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private CustomAiReportRequestRepository customAiReportRequestRepository;

  @Mock
  private CustomAiReportGenerationOutboxWriter outboxWriter;

  private PaymentCompletionService paymentCompletionService;

  @BeforeEach
  void setUp() {
    paymentCompletionService = new PaymentCompletionService(
        paymentRepository,
        customAiReportRequestRepository,
        outboxWriter
    );
  }

  @Test
  void completePayment_completesPendingPaymentAndRecordsOutbox() {
    Payment payment = pendingPayment(1L, 7L, "order-1", 3000L, 10L);
    CustomAiReportRequest customRequest = pendingCustomRequest(10L, 7L, 1L);

    given(paymentRepository.findByOrderIdAndUserId("order-1", 7L))
        .willReturn(Optional.of(payment));
    given(paymentRepository.findByPaymentKey("payment-key-1"))
        .willReturn(Optional.empty());
    given(customAiReportRequestRepository.findById(10L))
        .willReturn(Optional.of(customRequest));

    PaymentRes result = paymentCompletionService.completePayment(
        7L,
        new PaymentApproval("payment-key-1", "order-1", 3000L)
    );

    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    assertThat(payment.getPaymentKey()).isEqualTo("payment-key-1");
    assertThat(customRequest.getStatus()).isEqualTo(CustomAiReportRequestStatus.PAID);
    verify(outboxWriter).record(eq(10L), any());
  }

  @Test
  void completePayment_returnsExistingResult_whenAlreadyCompleted() {
    Payment payment = pendingPayment(1L, 7L, "order-1", 3000L, 10L);
    payment.complete("payment-key-1");

    given(paymentRepository.findByOrderIdAndUserId("order-1", 7L))
        .willReturn(Optional.of(payment));
    given(paymentRepository.findByPaymentKey("payment-key-1"))
        .willReturn(Optional.of(payment));

    PaymentRes result = paymentCompletionService.completePayment(
        7L,
        new PaymentApproval("payment-key-1", "order-1", 3000L)
    );

    assertThat(result.status()).isEqualTo("COMPLETED");
    verify(customAiReportRequestRepository, never()).findById(any());
    verify(outboxWriter, never()).record(any(), any());
  }

  @Test
  void completePayment_rejectsPaymentKeyUsedByAnotherPayment() {
    Payment current = pendingPayment(1L, 7L, "order-1", 3000L, 10L);
    Payment other = pendingPayment(2L, 8L, "order-2", 3000L, 11L);
    other.complete("payment-key-1");

    given(paymentRepository.findByOrderIdAndUserId("order-1", 7L))
        .willReturn(Optional.of(current));
    given(paymentRepository.findByPaymentKey("payment-key-1"))
        .willReturn(Optional.of(other));

    assertThatThrownBy(() -> paymentCompletionService.completePayment(
        7L,
        new PaymentApproval("payment-key-1", "order-1", 3000L)
    ))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DUPLICATE_PAYMENT);

    verify(outboxWriter, never()).record(any(), any());
  }

  @Test
  void completePayment_failsPayment_whenAmountMismatch() {
    Payment payment = pendingPayment(1L, 7L, "order-1", 3000L, 10L);

    given(paymentRepository.findByOrderIdAndUserId("order-1", 7L))
        .willReturn(Optional.of(payment));
    given(paymentRepository.findByPaymentKey("payment-key-1"))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCompletionService.completePayment(
        7L,
        new PaymentApproval("payment-key-1", "order-1", 1000L)
    ))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    verify(outboxWriter, never()).record(any(), any());
  }

  @Test
  void completePaymentByWebhook_usesOrderIdWithoutUserId() {
    Payment payment = pendingPayment(1L, 7L, "order-1", 3000L, 10L);
    CustomAiReportRequest customRequest = pendingCustomRequest(10L, 7L, 1L);

    given(paymentRepository.findByOrderId("order-1"))
        .willReturn(Optional.of(payment));
    given(paymentRepository.findByPaymentKey("payment-key-1"))
        .willReturn(Optional.empty());
    given(customAiReportRequestRepository.findById(10L))
        .willReturn(Optional.of(customRequest));

    PaymentRes result = paymentCompletionService.completePaymentByWebhook(
        "order-1",
        "payment-key-1",
        3000L
    );

    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    assertThat(customRequest.getStatus()).isEqualTo(CustomAiReportRequestStatus.PAID);
    verify(outboxWriter).record(eq(10L), any());
  }

  private Payment pendingPayment(Long id, Long userId, String orderId, long amount, Long targetId) {
    Payment payment = Payment.pending(
        userId,
        orderId,
        amount,
        "custom AI report",
        PaymentTargetType.CUSTOM_AI_REPORT,
        targetId
    );
    ReflectionTestUtils.setField(payment, "id", id);
    return payment;
  }

  private CustomAiReportRequest pendingCustomRequest(Long id, Long userId, Long paymentId) {
    CustomAiReportRequest request = CustomAiReportRequest.pendingPayment(
        10L,
        userId,
        List.of(new AiReportCustomPrompt(userId, "custom 1", "summary by user view"))
    );
    ReflectionTestUtils.setField(request, "id", id);
    request.linkPayment(paymentId);
    return request;
  }
}