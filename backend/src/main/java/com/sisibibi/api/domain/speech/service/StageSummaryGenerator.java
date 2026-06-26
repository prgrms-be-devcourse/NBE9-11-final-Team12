package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.speech.entity.Speech;

import java.util.List;

public interface StageSummaryGenerator {

    StageSummaryResult generate(Room room, List<Speech> speeches);
}
