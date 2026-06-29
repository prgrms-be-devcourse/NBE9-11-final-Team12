package com.sisibibi.api.domain.payment.repository;

import com.sisibibi.api.domain.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByOrderId(String orderId);

  boolean existsByOrderId(String orderId);

  Optional<Payment> findByPaymentKey(String paymentKey);


  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Payment> findByOrderIdAndUserId(String orderId, Long userId);

  List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}