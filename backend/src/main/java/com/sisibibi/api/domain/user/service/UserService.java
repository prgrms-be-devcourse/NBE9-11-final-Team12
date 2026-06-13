package com.sisibibi.api.domain.user.service;

import com.sisibibi.api.domain.user.dto.request.UpdateUserReq;
import com.sisibibi.api.domain.user.dto.response.UserMeRes;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserMeRes getMe(Long userId) {
        return UserMeRes.from(findUser(userId));
    }

    @Transactional
    public UserMeRes updateMe(Long userId, UpdateUserReq request) {
        User user = findUser(userId);
        user.changeNickname(request.nickname().trim());

        return UserMeRes.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
