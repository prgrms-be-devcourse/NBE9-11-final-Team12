package com.sisibibi.api.domain.auth.service;

import com.sisibibi.api.domain.auth.dto.request.SignupReq;
import com.sisibibi.api.domain.auth.dto.response.SignupRes;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
}
