package com.sisibibi.api.domain.speechreaction.repository;

import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpeechReactionRepository extends JpaRepository<SpeechReaction, Long> {

    boolean existsBySpeechIdAndUserId(Long speechId, Long userId);

    Optional<SpeechReaction> findBySpeechIdAndUserId(Long speechId, Long userId);
}
