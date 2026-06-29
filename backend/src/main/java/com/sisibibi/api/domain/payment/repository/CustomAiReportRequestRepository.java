package com.sisibibi.api.domain.payment.repository;

import com.sisibibi.api.domain.payment.entity.CustomAiReportRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CustomAiReportRequestRepository extends JpaRepository<CustomAiReportRequest, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<CustomAiReportRequest> findByIdAndUserId(Long id, Long userId);
}