package com.sisibibi.api.global.security.session;

import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenSessionValidator {

    private final UserRepository userRepository;

    public void validate(TokenClaims claims) {
        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new CustomException(ErrorCode.USER_INACTIVE);
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new CustomException(ErrorCode.USER_BANNED);
        }
        if (!user.getTokenVersion().equals(claims.tokenVersion())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
}
