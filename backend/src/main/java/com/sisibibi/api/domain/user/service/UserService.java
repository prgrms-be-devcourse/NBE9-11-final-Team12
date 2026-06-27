package com.sisibibi.api.domain.user.service;

import com.sisibibi.api.domain.user.dto.request.UpdateUserReq;
import com.sisibibi.api.domain.user.dto.response.AdminUserRes;
import com.sisibibi.api.domain.user.dto.response.UserMeRes;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.entity.UserRole;
import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public Page<AdminUserRes> getUsersForAdmin(
            String keyword,
            UserStatus status,
            UserRole role,
            Pageable pageable
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);

        return userRepository.searchForAdmin(normalizedKeyword, status, role, pageable)
                .map(AdminUserRes::from);
    }

    @Transactional(readOnly = true)
    public AdminUserRes getUserForAdmin(Long userId) {
        return AdminUserRes.from(findUser(userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }
}
