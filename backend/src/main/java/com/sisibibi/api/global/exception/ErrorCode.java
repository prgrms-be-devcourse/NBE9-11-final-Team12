package com.sisibibi.api.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // User
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),
    USER_BANNED(HttpStatus.FORBIDDEN, "차단된 계정입니다."),

    // Topic
    TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 토픽입니다."),
    TOPIC_ALREADY_APPROVED(HttpStatus.BAD_REQUEST, "이미 승인된 토픽입니다."),
    TOPIC_ALREADY_REJECTED(HttpStatus.BAD_REQUEST, "이미 반려된 토픽입니다."),
    TOPIC_NOT_APPROVED(HttpStatus.BAD_REQUEST, "승인되지 않은 토픽입니다."),
    DUPLICATE_TOPIC(HttpStatus.CONFLICT, "이미 존재하는 토픽입니다."),

    // Room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 토론방입니다."),
    ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 토픽으로 생성된 토론방이 존재합니다."),
    ROOM_CLOSED(HttpStatus.BAD_REQUEST, "이미 종료된 토론방입니다."),
    ROOM_NOT_OPEN(HttpStatus.BAD_REQUEST, "현재 입장 가능한 토론방이 아닙니다."),
    ROOM_ALREADY_PARTICIPATED(HttpStatus.CONFLICT, "이미 참여 중인 토론방입니다."),

    // Chat
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채팅 메시지입니다."),
    CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "채팅 메시지는 비어 있을 수 없습니다."),
    CHAT_MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "채팅 메시지 길이를 초과했습니다."),

    // Speaking Queue
    SPEAKING_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 발언권을 신청한 상태입니다."),
    SPEAKING_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "발언권 신청 내역이 존재하지 않습니다."),
    SPEAKING_QUEUE_ORDER_CONFLICT(HttpStatus.CONFLICT, "발언권 대기 순번 충돌이 발생했습니다. 다시 시도해주세요."),
    SPEAKING_QUEUE_EMPTY(HttpStatus.BAD_REQUEST, "발언권 대기열이 비어 있습니다."),
    CURRENT_SPEAKER_ALREADY_EXISTS(HttpStatus.CONFLICT, "현재 발언자가 이미 존재합니다."),
    CURRENT_SPEAKER_NOT_FOUND(HttpStatus.NOT_FOUND, "현재 발언자가 존재하지 않습니다."),
    SPEAKING_TIME_EXPIRED(HttpStatus.BAD_REQUEST, "발언 시간이 만료되었습니다."),
    SPEAKING_TIME_NOT_EXPIRED(HttpStatus.BAD_REQUEST, "발언 시간이 아직 만료되지 않았습니다."),

    // Speech
    SPEECH_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 발언입니다."),
    SPEECH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 발언에 접근할 권한이 없습니다."),
    SPEECH_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "수정할 수 없는 발언 상태입니다."),
    INVALID_STANCE(HttpStatus.BAD_REQUEST, "올바르지 않은 입장 값입니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다."),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 결제 내역입니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "결제에 실패했습니다."),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 결제입니다."),
    PAYMENT_ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 취소된 결제입니다."),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "중복 결제 요청입니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_REQUEST, "결제 승인에 실패했습니다."),

    // File
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기 제한을 초과했습니다.");

    private final HttpStatus status;
    private final String message;
}
