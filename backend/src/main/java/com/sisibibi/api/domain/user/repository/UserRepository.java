package com.sisibibi.api.domain.user.repository;

import com.sisibibi.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
