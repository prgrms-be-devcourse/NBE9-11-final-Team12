package com.sisibibi.api.domain.auth.service;

import com.sisibibi.api.domain.auth.dto.request.SignupReq;
import com.sisibibi.api.domain.auth.dto.request.LoginReq;
import com.sisibibi.api.domain.auth.dto.response.SignupRes;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenClaims;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenType;
import com.sisibibi.api.global.security.refresh.RefreshTokenStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private AuthService authService;

    @Test
    void signup_createsUserWithEncodedPassword() {
        SignupReq request = new SignupReq("user@example.com", "password123!", "tester");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123!")).willReturn("encoded-password");
        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        SignupRes response = authService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getNickname()).isEqualTo("tester");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("tester");
    }

    @Test
    void signup_throwsDuplicateEmail_whenEmailAlreadyExists() {
        SignupReq request = new SignupReq("user@example.com", "password123!", "tester");
        given(userRepository.findByEmail("user@example.com"))
                .willReturn(Optional.of(User.signup("user@example.com", "encoded", "tester")));

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void login_issuesTokens_whenCredentialsAreValid() {
        User user = User.signup("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123!", "encoded-password")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(any())).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(any(), anyString())).willReturn("refresh-token");

        var result = authService.login(new LoginReq("user@example.com", "password123!"));

        assertThat(result.response().userId()).isEqualTo(1L);
        assertThat(result.response().email()).isEqualTo("user@example.com");
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenStore).save(eq(1L), anyString(), eq("refresh-token"));
        ArgumentCaptor<com.sisibibi.api.global.security.AuthPrincipal> principalCaptor =
                ArgumentCaptor.forClass(com.sisibibi.api.global.security.AuthPrincipal.class);
        verify(jwtTokenProvider).createAccessToken(principalCaptor.capture());
        assertThat(principalCaptor.getValue().tokenVersion()).isEqualTo(0L);
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordDoesNotMatch() {
        User user = User.signup("user@example.com", "encoded-password", "tester");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginReq("user@example.com", "wrong-password")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(refreshTokenStore, never()).save(any(), anyString(), anyString());
    }

    @Test
    void login_throwsInvalidCredentials_whenEmailDoesNotExist() {
        given(userRepository.findByEmail("missing@example.com"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.login(new LoginReq("missing@example.com", "password123!"))
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(refreshTokenStore, never()).save(any(), anyString(), anyString());
    }

    @Test
    void login_throwsUserInactive_whenUserIsInactive() {
        User user = User.signup("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "status", UserStatus.INACTIVE);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123!", "encoded-password")).willReturn(true);

        assertThatThrownBy(() ->
                authService.login(new LoginReq("user@example.com", "password123!"))
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_INACTIVE);

        verify(refreshTokenStore, never()).save(any(), anyString(), anyString());
    }

    @Test
    void login_throwsUserBanned_whenUserIsBanned() {
        User user = User.signup("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "status", UserStatus.BANNED);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123!", "encoded-password")).willReturn(true);

        assertThatThrownBy(() ->
                authService.login(new LoginReq("user@example.com", "password123!"))
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BANNED);

        verify(refreshTokenStore, never()).save(any(), anyString(), anyString());
    }

    @Test
    void reissue_rotatesRefreshToken() {
        TokenClaims claims = new TokenClaims(
                1L,
                "user@example.com",
                "USER",
                "old-token-id",
                TokenType.REFRESH,
                Instant.parse("2030-06-12T00:00:00Z")
        );
        User user = User.signup("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(jwtTokenProvider.parseRefreshToken("old-refresh-token")).willReturn(claims);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(any())).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(any(), anyString())).willReturn("new-refresh-token");

        var result = authService.reissue("old-refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenStore).verifyAndDelete(1L, "old-token-id", "old-refresh-token");
        verify(refreshTokenStore).save(eq(1L), anyString(), eq("new-refresh-token"));
    }

    @Test
    void logout_deletesRefreshToken() {
        TokenClaims claims = new TokenClaims(
                1L,
                "user@example.com",
                "USER",
                "token-id",
                TokenType.REFRESH,
                Instant.parse("2030-06-12T00:00:00Z")
        );
        given(jwtTokenProvider.parseRefreshToken("refresh-token")).willReturn(claims);

        authService.logout("refresh-token");

        verify(refreshTokenStore).delete(1L, "token-id");
    }

    @Test
    void reissue_throwsUserNotFound_whenTokenOwnerDoesNotExist() {
        TokenClaims claims = refreshClaims();
        given(jwtTokenProvider.parseRefreshToken("old-refresh-token")).willReturn(claims);
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue("old-refresh-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(refreshTokenStore).verifyAndDelete(
                1L,
                "old-token-id",
                "old-refresh-token"
        );
        verify(refreshTokenStore, never()).save(any(), anyString(), anyString());
    }

    @Test
    void reissue_throwsUserBanned_whenTokenOwnerIsBanned() {
        TokenClaims claims = refreshClaims();
        User user = User.signup("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "status", UserStatus.BANNED);
        given(jwtTokenProvider.parseRefreshToken("old-refresh-token")).willReturn(claims);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.reissue("old-refresh-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BANNED);

        verify(refreshTokenStore, never()).save(any(), anyString(), anyString());
    }

    @Test
    void reissue_throwsInvalidToken_whenTokenVersionIsOld() {
        TokenClaims claims = new TokenClaims(
                1L,
                "user@example.com",
                "USER",
                "old-token-id",
                TokenType.REFRESH,
                0L,
                Instant.parse("2030-06-12T00:00:00Z")
        );
        User user = User.signup("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.invalidateTokens();
        given(jwtTokenProvider.parseRefreshToken("old-refresh-token")).willReturn(claims);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.reissue("old-refresh-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);

        verify(refreshTokenStore, never()).save(any(), anyString(), anyString());
    }

    private TokenClaims refreshClaims() {
        return new TokenClaims(
                1L,
                "user@example.com",
                "USER",
                "old-token-id",
                TokenType.REFRESH,
                Instant.parse("2030-06-12T00:00:00Z")
        );
    }
}
