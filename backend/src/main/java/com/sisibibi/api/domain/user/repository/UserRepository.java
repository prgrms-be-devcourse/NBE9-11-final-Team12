package com.sisibibi.api.domain.user.repository;

import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.entity.UserRole;
import com.sisibibi.api.domain.user.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    @Query("""
            select user
            from User user
            where (:keyword is null
                   or lower(user.email) like lower(concat('%', :keyword, '%'))
                   or lower(user.nickname) like lower(concat('%', :keyword, '%')))
              and (:status is null or user.status = :status)
              and (:role is null or user.role = :role)
            """)
    Page<User> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            @Param("role") UserRole role,
            Pageable pageable
    );
}
