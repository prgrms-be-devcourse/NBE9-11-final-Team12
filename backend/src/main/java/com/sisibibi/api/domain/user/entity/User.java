package com.sisibibi.api.domain.user.entity;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    private User(UserStatus status) {
        this.status = status;
    }

    public static User active() {
        return new User(UserStatus.ACTIVE);
    }

    public static User banned() {
        return new User(UserStatus.BANNED);
    }

    public void validateCanRequestSpeakingTurn() {
        if (status == UserStatus.BANNED) {
            throw new CustomException(ErrorCode.USER_BANNED);
        }
    }
}
