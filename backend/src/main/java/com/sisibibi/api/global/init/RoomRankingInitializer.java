package com.sisibibi.api.global.init;

import com.sisibibi.api.domain.roomranking.service.RoomRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomRankingInitializer implements ApplicationRunner {

  private final RoomRankingService roomRankingService;

  @Override
  public void run(ApplicationArguments args) {
    roomRankingService.rebuildOpenRoomRanking();
  }
}
