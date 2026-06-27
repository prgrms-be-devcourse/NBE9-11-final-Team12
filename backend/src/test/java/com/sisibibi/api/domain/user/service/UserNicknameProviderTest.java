package com.sisibibi.api.domain.user.service;

import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserNicknameProviderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserNicknameProvider userNicknameProvider;

    @Test
    void findNicknamesByIds_returnsNicknameMap() {
        given(userRepository.findAllById(argThat(ids -> {
            Set<Long> actualUserIds = new HashSet<>();
            ids.forEach(actualUserIds::add);
            return actualUserIds.equals(Set.of(1L, 2L));
        })))
                .willReturn(List.of(user(1L, "사용자1"), user(2L, "사용자2")));

        var response = userNicknameProvider.findNicknamesByIds(Arrays.asList(1L, 2L, 1L, null));

        assertThat(response).containsEntry(1L, "사용자1");
        assertThat(response).containsEntry(2L, "사용자2");
    }

    @Test
    void findNicknamesByIds_returnsEmptyMap_whenUserIdsAreEmpty() {
        var response = userNicknameProvider.findNicknamesByIds(Arrays.asList(null, null));

        assertThat(response).isEmpty();
        verify(userRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
    }

    private User user(Long id, String nickname) {
        User user = User.signup("user" + id + "@example.com", "password", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
