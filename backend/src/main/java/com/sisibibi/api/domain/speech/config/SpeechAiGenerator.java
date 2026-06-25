package com.sisibibi.api.domain.speech.config;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.speech.entity.SpeechStance;

public interface SpeechAiGenerator {
    String generate(Room room, SpeechStance targetStance);

}
