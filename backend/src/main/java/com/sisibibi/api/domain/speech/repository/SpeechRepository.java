package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.Speech;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeechRepository extends JpaRepository<Speech, Long> {
}
