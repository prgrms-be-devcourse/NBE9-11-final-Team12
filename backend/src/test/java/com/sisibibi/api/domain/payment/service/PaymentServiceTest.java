package com.sisibibi.api.domain.payment.service;

import com.sisibibi.api.domain.payment.client.PaymentApproval;
import com.sisibibi.api.domain.payment.client.PaymentClient;
import com.sisibibi.api.domain.payment.dto.request.ConfirmPaymentReq;
import com.sisibibi.api.domain.payment.dto.request.CreateCustomAiReportPaymentReq;
import com.sisibibi.api.domain.payment.dto.response.PaymentRes;
import com.sisibibi.api.domain.payment.entity.CustomAiReportRequest;
import com.sisibibi.api.domain.payment.entity.Payment;
import com.sisibibi.api.domain.payment.entity.PaymentStatus;
import com.sisibibi.api.domain.payment.entity.PaymentTargetType;
import com.sisibibi.api.domain.payment.repository.CustomAiReportRequestRepository;
import com.sisibibi.api.domain.payment.repository.PaymentRepository;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
import com.sisibibi.api.domain.report.prompt.CustomPromptCommand;
import com.sisibibi.api.domain.report.prompt.CustomPromptValidator;
import com.sisibibi.api.domain.report.repository.AiReportRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private CustomAiReportRequestRepository customAiReportRequestRepository;

  @Mock
  private AiReportRepository aiReportRepository;

  @Mock
  private CustomPromptValidator customPromptValidator;

  @Mock
  private PaymentClient paymentClient;

  @Mock
  private PaymentCompletionService paymentCompletionService;

  private PaymentService paymentService;

  @BeforeEach
  void setUp() {
    paymentService = new PaymentService(
        paymentRepository,
        customAiReportRequestRepository,
        aiReportRepository,
        customPromptValidator,
        paymentClient,
        paymentCompletionService
    );
  }

  @Test
  void createCustomAiReportPayment_createsPendingPayment_whenBaseReportCompletedAndAmountValid() {
    Long userId = 7L;
    Long roomId = 10L;
    AiReport report = completedAiReport(roomId);

    CreateCustomAiReportPaymentReq request = new CreateCustomAiReportPaymentReq(
        roomId,
        3000L,
        List.of(new AiReportGenerateReq.CustomPromptReq("custom 1", "summarize my view"))
    );

    given(aiReportRepository.findByRoomId(roomId)).willReturn(Optional.of(report));
    given(customPromptValidator.normalizeAndScan(any(AiReportGenerateReq.class)))
        .willReturn(List.of(new CustomPromptCommand("custom 1", "summarize my view")));
    given(customAiReportRequestRepository.save(any(CustomAiReportRequest.class)))
        .willAnswer(invocation -> {
          CustomAiReportRequest saved = invocation.getArgument(0);
          ReflectionTestUtils.setField(saved, "id", 11L);
          return saved;
        });
    given(paymentRepository.save(any(Payment.class)))
        .willAnswer(invocation -> {
          Payment saved = invocation.getArgument(0);
          ReflectionTestUtils.setField(saved, "id", 22L);
          return saved;
        });

    PaymentRes result = paymentService.createCustomAiReportPayment(userId, request);

    ArgumentCaptor<CustomAiReportRequest> customRequestCaptor =
        ArgumentCaptor.forClass(CustomAiReportRequest.class);
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

    verify(customAiReportRequestRepository).save(customRequestCaptor.capture());
    verify(paymentRepository).save(paymentCaptor.capture());

    assertThat(result.paymentId()).isEqualTo(22L);
    assertThat(result.status()).isEqualTo("PENDING");
    assertThat(result.amount()).isEqualTo(3000L);
    assertThat(result.targetType()).isEqualTo("CUSTOM_AI_REPORT");

    assertThat(customRequestCaptor.getValue().getRoomId()).isEqualTo(roomId);
    assertThat(customRequestCaptor.getValue().getUserId()).isEqualTo(userId);
    assertThat(customRequestCaptor.getValue().getCustomPrompts()).containsExactly(
        new AiReportCustomPrompt(userId, "custom 1", "summarize my view")
    );

    assertThat(paymentCaptor.getValue().getUserId()).isEqualTo(userId);
    assertThat(paymentCaptor.getValue().getAmount()).isEqualTo(3000L);
    assertThat(paymentCaptor.getValue().getTargetType()).isEqualTo(PaymentTargetType.CUSTOM_AI_REPORT);
    assertThat(paymentCaptor.getValue().getTargetId()).isEqualTo(11L);
  }

  @Test
  void createCustomAiReportPayment_rejectsClientControlledWrongAmount() {
    CreateCustomAiReportPaymentReq request = new CreateCustomAiReportPaymentReq(
        10L,
        1L,
        List.of(new AiReportGenerateReq.CustomPromptReq("custom 1", "cheap request"))
    );

    given(aiReportRepository.findByRoomId(10L)).willReturn(Optional.of(completedAiReport(10L)));

    assertThatThrownBy(() -> paymentService.createCustomAiReportPayment(7L, request))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

    verify(customAiReportRequestRepository, never()).save(any());
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void confirmPayment_returnsExistingResult_whenAlreadyCompleted() {
    Payment payment = pendingPayment(22L, 7L, "order-1", 3000L, 11L);
    payment.complete("payment-key-1");

    ConfirmPaymentReq request = new ConfirmPaymentReq("order-1", "payment-key-1", 3000L);

    given(paymentRepository.findByPaymentKey("payment-key-1")).willReturn(Optional.of(payment));
    given(paymentRepository.findByOrderIdAndUserId("order-1", 7L)).willReturn(Optional.of(payment));

    PaymentRes result = paymentService.confirmPayment(7L, request);

    assertThat(result.status()).isEqualTo("COMPLETED");
    verify(paymentClient, never()).confirm(any(), any(), any(Long.class));
    verify(paymentCompletionService, never()).completePayment(any(), any());
  }

  @Test
  void confirmPayment_callsTossAndDelegatesCompletion_whenPendingPaymentIsValid() {
    Payment payment = pendingPayment(22L, 7L, "order-1", 3000L, 11L);
    ConfirmPaymentReq request = new ConfirmPaymentReq("order-1", "payment-key-1", 3000L);
    PaymentApproval approval = new PaymentApproval("payment-key-1", "order-1", 3000L);
    PaymentRes completed = new PaymentRes(
        22L,
        "order-1",
        "payment-key-1",
        3000L,
        "custom AI report",
        "COMPLETED",
        "CUSTOM_AI_REPORT",
        11L,
        null,
        null
    );

    given(paymentRepository.findByPaymentKey("payment-key-1")).willReturn(Optional.empty());
    given(paymentRepository.findByOrderIdAndUserId("order-1", 7L)).willReturn(Optional.of(payment));
    given(paymentClient.confirm("payment-key-1", "order-1", 3000L)).willReturn(approval);
    given(paymentCompletionService.completePayment(7L, approval)).willReturn(completed);

    PaymentRes result = paymentService.confirmPayment(7L, request);

    assertThat(result.status()).isEqualTo("COMPLETED");
    verify(paymentClient).confirm("payment-key-1", "order-1", 3000L);
    verify(paymentCompletionService).completePayment(7L, approval);
  }

  @Test
  void confirmPayment_rejectsPaymentKeyUsedByAnotherOrder() {
    Payment other = pendingPayment(33L, 8L, "order-2", 3000L, 12L);
    other.complete("payment-key-1");

    ConfirmPaymentReq request = new ConfirmPaymentReq("order-1", "payment-key-1", 3000L);

    given(paymentRepository.findByPaymentKey("payment-key-1")).willReturn(Optional.of(other));

    assertThatThrownBy(() -> paymentService.confirmPayment(7L, request))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DUPLICATE_PAYMENT);

    verify(paymentClient, never()).confirm(any(), any(), any(Long.class));
    verify(paymentCompletionService, never()).completePayment(any(), any());
  }

  private AiReport completedAiReport(Long roomId) {
    AiReport report = AiReport.requested(roomId);
    ReflectionTestUtils.setField(report, "id", 55L);
    report.complete(new AiReportGenerateRes(
        "core",
        List.of("issue"),
        "summary",
        "common",
        "opinion"
    ));
    return report;
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
}