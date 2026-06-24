package com.sisibibi.api.domain.auth.service;

import com.sisibibi.api.domain.auth.dto.request.LoginReq;
import com.sisibibi.api.domain.auth.dto.request.SignupReq;
import com.sisibibi.api.domain.auth.dto.response.AuthTokenResult;
import com.sisibibi.api.domain.auth.dto.response.LoginRes;
import com.sisibibi.api.domain.auth.dto.response.SignupRes;
import com.sisibibi.api.domain.auth.dto.response.TokenReissueRes;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.AuthPrincipal;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenClaims;
import com.sisibibi.api.global.security.refresh.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public SignupRes signup(SignupReq request) {
        userRepository.findByEmail(request.email())
                .ifPresent(user -> {
                    throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
                });

        User user = User.signup(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );

        return SignupRes.from(userRepository.save(user));
    }

    @Transactional
    public AuthTokenResult<LoginRes> login(LoginReq request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        validateLoginAvailable(user);

        return issueTokens(user, LoginRes.from(user));
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        try {
            TokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
            refreshTokenStore.delete(claims.userId(), claims.tokenId());
        } catch (CustomException tokenException) {
            if (!isIgnorableLogoutTokenError(tokenException.getErrorCode())) {
                throw tokenException;
            }
        }
    }

    @Transactional
    public AuthTokenResult<TokenReissueRes> reissue(String refreshToken) {
        TokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        refreshTokenStore.verifyAndDelete(claims.userId(), claims.tokenId(), refreshToken);

        User user = userRepository.findByIdForUpdate(claims.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateLoginAvailable(user);
        validateTokenVersion(user, claims);

        return issueTokens(user, TokenReissueRes.from(user));
    }

    private <T> AuthTokenResult<T> issueTokens(User user, T response) {
        String refreshTokenId = UUID.randomUUID().toString();
        AuthPrincipal principal = new AuthPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getTokenVersion()
        );

        String accessToken = jwtTokenProvider.createAccessToken(principal);
        String refreshToken = jwtTokenProvider.createRefreshToken(principal, refreshTokenId);
        refreshTokenStore.save(user.getId(), refreshTokenId, refreshToken);

        return new AuthTokenResult<>(response, accessToken, refreshToken);
    }

    private void validateLoginAvailable(User user) {
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new CustomException(ErrorCode.USER_INACTIVE);
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new CustomException(ErrorCode.USER_BANNED);
        }
    }

    private void validateTokenVersion(User user, TokenClaims claims) {
        if (!user.getTokenVersion().equals(claims.tokenVersion())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private boolean isIgnorableLogoutTokenError(ErrorCode errorCode) {
        return errorCode == ErrorCode.INVALID_TOKEN
                || errorCode == ErrorCode.EXPIRED_TOKEN;
    }
}
