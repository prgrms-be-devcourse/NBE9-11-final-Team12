package com.sisibibi.api.global.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ApiResponse<T> {

    private int status;
    private String code;
    private String message;
    private T data;


    // 200 OK, 조회 성공시 사용
    // 사용 예시: return ResponseEntity.ok(ApiResponse.ok(userDto));
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                "SUCCESS",
                "요청에 성공했습니다.",
                data
        );
    }

    // 200 OK, 조회/수정 성공 시 커스텀 메시지가 필요한 경우 사용
    // 사용 예시: return ResponseEntity.ok(ApiResponse.ok("회원 조회가 완료되었습니다.", userDto));
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                "SUCCESS",
                message,
                data
        );
    }

    // 200 OK, 삭제 성공/상태 변경 성공 등 반환 데이터가 없는 경우 사용
    // 사용 예시 : return ResponseEntity.ok(ApiResponse.okMessage("토론방 삭제가 완료되었습니다."));
    public static ApiResponse<Void> okMessage(String message) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                "SUCCESS",
                message,
                null
        );
    }

    // 201 CREATED, 생성 성공 시 사용
    // 사용 예시: return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("토론방 생성이 완료되었습니다.", response));
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "SUCCESS",
                message,
                data
        );
    }

    // 201 CREATED, 생성 성공 후 반환 데이터가 없는 경우 사용
    // 사용 예시: return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.createdMessage("토론방 생성이 완료되었습니다."));
    public static ApiResponse<Void> createdMessage(String message) {
        return new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "SUCCESS",
                message,
                null
        );
    }

    // 에러 응답 생성, data가 필요 없는 경우 사용
    // 사용 예시: ApiResponse.error(HttpStatus.NOT_FOUND,"ROOM_NOT_FOUND","존재하지 않는 토론방입니다.");
    public static ApiResponse<Void> error(HttpStatus status, String code, String message) {
        return new ApiResponse<>(
                status.value(),
                code,
                message,
                null
        );
    }

    // 에러 응답 생성, Validation 오류처럼 추가 데이터를 포함해야 하는 경우 사용
    // 사용 예시: ApiResponse.error(HttpStatus.BAD_REQUEST,"INVALID_INPUT_VALUE","잘못된 입력값입니다.", errors);
    public static <T> ApiResponse<T> error(
            HttpStatus status,
            String code,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                status.value(),
                code,
                message,
                data
        );
    }
}