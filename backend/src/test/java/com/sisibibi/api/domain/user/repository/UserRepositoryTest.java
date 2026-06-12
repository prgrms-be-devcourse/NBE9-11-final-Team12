package com.sisibibi.api.domain.user.repository;

import com.sisibibi.api.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_returnsUser() {
        userRepository.saveAndFlush(User.signup("user@example.com", "encoded-password", "tester"));

        assertThat(userRepository.findByEmail("user@example.com"))
                .isPresent()
                .get()
                .extracting(User::getNickname)
                .isEqualTo("tester");
    }

    @Test
    void save_rejectsDuplicateEmail() {
        userRepository.saveAndFlush(User.signup("user@example.com", "encoded-password", "tester"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                User.signup("user@example.com", "encoded-password-2", "tester2")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
