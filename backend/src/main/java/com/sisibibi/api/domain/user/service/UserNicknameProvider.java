package com.sisibibi.api.domain.user.service;

import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserNicknameProvider {

    private final UserRepository userRepository;

    public Map<Long, String> findNicknamesByIds(Collection<Long> userIds) {
        Set<Long> uniqueUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (uniqueUserIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(uniqueUserIds)
                .stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (left, right) -> left));
    }
}
