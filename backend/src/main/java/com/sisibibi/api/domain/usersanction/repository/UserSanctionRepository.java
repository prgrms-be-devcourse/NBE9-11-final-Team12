package com.sisibibi.api.domain.usersanction.repository;

import com.sisibibi.api.domain.usersanction.entity.UserSanction;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserSanctionRepository extends JpaRepository<UserSanction, Long> {

    @Query("""
            select count(sanction) > 0
            from UserSanction sanction
            where sanction.userId = :userId
              and sanction.type = :type
              and sanction.revokedAt is null
              and sanction.startsAt <= :now
              and sanction.endsAt > :now
            """)
    boolean existsActive(
            @Param("userId") Long userId,
            @Param("type") UserSanctionType type,
            @Param("now") LocalDateTime now
    );

    Page<UserSanction> findByUserIdOrderByCreatedAtDescIdDesc(
            Long userId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sanction
            from UserSanction sanction
            where sanction.id = :sanctionId
              and sanction.userId = :userId
            """)
    Optional<UserSanction> findByIdAndUserIdForUpdate(
            @Param("sanctionId") Long sanctionId,
            @Param("userId") Long userId
    );
}
