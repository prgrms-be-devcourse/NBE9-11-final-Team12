package com.sisibibi.api.domain.user.service;

import com.sisibibi.api.domain.user.dto.request.UpdateUserReq;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.entity.UserRole;
import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getMe_returnsCurrentUser() {
        User user = User.signup("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        var response = userService.getMe(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("tester");
    }

    @Test
    void updateMe_changesNickname() {
        User user = User.signup("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        var response = userService.updateMe(1L, new UpdateUserReq("newbie"));

        assertThat(user.getNickname()).isEqualTo("newbie");
        assertThat(response.nickname()).isEqualTo("newbie");
    }

    @Test
    void getMe_throwsUserNotFound_whenUserDoesNotExist() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void getUsersForAdmin_returnsFilteredUsers() {
        User user = User.signup("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        var pageable = PageRequest.of(0, 20);
        given(userRepository.searchForAdmin(
                eq("tester"),
                eq(UserStatus.ACTIVE),
                eq(UserRole.USER),
                eq(pageable)
        )).willReturn(new PageImpl<>(List.of(user)));

        var response = userService.getUsersForAdmin(
                " tester ",
                UserStatus.ACTIVE,
                UserRole.USER,
                pageable
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).email()).isEqualTo("user@example.com");
        assertThat(response.getContent().get(0).nickname()).isEqualTo("tester");
    }

    @Test
    void getUsersForAdmin_normalizesBlankKeywordToNull() {
        var pageable = PageRequest.of(0, 20);
        given(userRepository.searchForAdmin(
                isNull(),
                isNull(),
                isNull(),
                eq(pageable)
        )).willReturn(new PageImpl<>(List.of()));

        var response = userService.getUsersForAdmin(" ", null, null, pageable);

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    void getUserForAdmin_returnsUserWithoutPassword() {
        User user = User.signup("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        var response = userService.getUserForAdmin(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("tester");
    }
}
