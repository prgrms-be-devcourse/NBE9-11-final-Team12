package com.sisibibi.api.global.init;

import com.sisibibi.api.domain.roomranking.service.RoomRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(
    name = "room-ranking.rebuild-on-startup",
    havingValue = "true"
)
@Component
@RequiredArgsConstructor
public class RoomRankingInitializer implements ApplicationRunner {

  private final RoomRankingService roomRankingService;

  // 초기 db 랭킹 세팅
  @Override
  public void run(ApplicationArguments args) {
    roomRankingService.rebuildOpenRoomRanking();
  }
}
