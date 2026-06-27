package com.sisibibi.api.domain.user.entity;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "token_version", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long tokenVersion;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static User signup(String email, String encodedPassword, String nickname) {
        return create(email, encodedPassword, nickname, UserRole.USER);
    }

    public static User admin(String email, String encodedPassword, String nickname) {
        return create(email, encodedPassword, nickname, UserRole.ADMIN);
    }

    private static User create(String email, String encodedPassword, String nickname, UserRole role) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.nickname = nickname;
        user.role = role;
        user.status = UserStatus.ACTIVE;
        user.tokenVersion = 0L;
        return user;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void ban() {
        if (status == UserStatus.BANNED) {
            throw new CustomException(ErrorCode.USER_BANNED);
        }
        this.status = UserStatus.BANNED;
        invalidateTokens();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void invalidateTokens() {
        this.tokenVersion++;
    }
}
