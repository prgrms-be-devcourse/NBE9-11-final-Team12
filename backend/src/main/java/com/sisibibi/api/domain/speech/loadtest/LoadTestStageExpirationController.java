package com.sisibibi.api.domain.speech.loadtest;

import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Profile("load-test")
@RequiredArgsConstructor
@RequestMapping("/api/load-test/stage/expiration")
public class LoadTestStageExpirationController {

    private final LoadTestStageExpirationService loadTestStageExpirationService;

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<LoadTestExpirationPrepareRes>> prepareExpirationCandidates(
            @Positive @RequestParam(defaultValue = "10") int roomCount,
            @Positive @RequestParam(defaultValue = "1") int waitingPerRoom,
            @Positive @RequestParam(defaultValue = "1") Long roomIdStart,
            @Positive @RequestParam(defaultValue = "1") Long userIdStart
    ) {
        LoadTestExpirationPrepareRes response = loadTestStageExpirationService
                .prepareExpirationCandidates(
                        roomCount,
                        waitingPerRoom,
                        roomIdStart,
                        userIdStart
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("자동 만료 부하 테스트 데이터 준비가 완료되었습니다.", response));
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<LoadTestExpirationRunRes>> runExpiration() {
        LoadTestExpirationRunRes response = loadTestStageExpirationService.runExpiration();

        return ResponseEntity.ok(ApiResponse.ok("자동 만료 부하 테스트 실행이 완료되었습니다.", response));
    }

    @PostMapping("/race/prepare")
    public ResponseEntity<ApiResponse<LoadTestExpirationRacePrepareRes>> prepareExpirationRace(
            @Positive @RequestParam(defaultValue = "1") Long roomId,
            @Positive @RequestParam(defaultValue = "1") Long currentSpeakerUserId,
            @Positive @RequestParam(defaultValue = "2") Long nextSpeakerUserId
    ) {
        LoadTestExpirationRacePrepareRes response = loadTestStageExpirationService
                .prepareExpirationRace(
                        roomId,
                        currentSpeakerUserId,
                        nextSpeakerUserId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("종료와 자동 만료 경합 테스트 데이터 준비가 완료되었습니다.", response));
    }

    @PostMapping("/race/verify")
    public ResponseEntity<ApiResponse<LoadTestExpirationRaceVerifyRes>> verifyExpirationRace(
            @Positive @RequestParam(defaultValue = "1") Long roomId
    ) {
        LoadTestExpirationRaceVerifyRes response = loadTestStageExpirationService
                .verifyExpirationRace(roomId);

        return ResponseEntity.ok(ApiResponse.ok("종료와 자동 만료 경합 테스트 검증이 완료되었습니다.", response));
    }
}
