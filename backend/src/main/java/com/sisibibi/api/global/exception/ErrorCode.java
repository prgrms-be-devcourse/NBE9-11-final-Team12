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
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 인증 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh Token을 찾을 수 없습니다."),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "이미 사용된 Refresh Token입니다."),

    // User
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),
    USER_BANNED(HttpStatus.FORBIDDEN, "차단된 계정입니다."),
    USER_SANCTION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "동일한 유형의 활성 제재가 이미 존재합니다."),
    USER_SANCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자 제재입니다."),
    USER_SANCTION_NOT_REVOCABLE(HttpStatus.CONFLICT, "활성 상태의 사용자 제재만 해제할 수 있습니다."),
    USER_SANCTION_NOT_EXTENDABLE(HttpStatus.CONFLICT, "현재 활성 제재보다 종료 시각이 늦어지는 경우에만 연장할 수 있습니다."),
    USER_SANCTION_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "올바르지 않은 사용자 제재 기간입니다."),
    USER_SANCTION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "사용자 제재 사유가 필요합니다."),
    USER_SANCTION_REASON_TOO_LONG(HttpStatus.BAD_REQUEST, "사용자 제재 사유는 500자 이하여야 합니다."),
    USER_CHAT_RESTRICTED(HttpStatus.FORBIDDEN, "채팅 이용이 제한된 사용자입니다."),
    USER_SPEECH_RESTRICTED(HttpStatus.FORBIDDEN, "의견 작성이 제한된 사용자입니다."),
    USER_STAGE_RESTRICTED(HttpStatus.FORBIDDEN, "발언권 신청이 제한된 사용자입니다."),
    USER_SANCTION_REPORT_MISMATCH(HttpStatus.BAD_REQUEST, "신고 대상 사용자와 제재 대상 사용자가 일치하지 않습니다."),
    USER_SANCTION_REPORT_NOT_RESOLVED(HttpStatus.BAD_REQUEST, "처리 완료된 신고만 사용자 제재와 연결할 수 있습니다."),
    USER_SANCTION_ADMIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "관리자 계정은 사용자 제재 대상으로 지정할 수 없습니다."),

    // Topic
    TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 토픽입니다."),
    TOPIC_ALREADY_APPROVED(HttpStatus.BAD_REQUEST, "이미 승인된 토픽입니다."),
    TOPIC_ALREADY_REJECTED(HttpStatus.BAD_REQUEST, "이미 반려된 토픽입니다."),
    TOPIC_NOT_APPROVED(HttpStatus.BAD_REQUEST, "승인되지 않은 토픽입니다."),
    DUPLICATE_TOPIC(HttpStatus.CONFLICT, "이미 존재하는 토픽입니다."),
    TOPIC_HAS_ROOM(HttpStatus.CONFLICT, "토론방과 연결된 토픽은 삭제할 수 없습니다."),
    NAVER_SEARCH_CONFIG_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "네이버 검색 API 설정이 누락되었습니다."),
    NAVER_SEARCH_FAILED(HttpStatus.BAD_GATEWAY, "네이버 뉴스 검색에 실패했습니다."),
    SERPAPI_CONFIG_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "SerpApi 설정이 누락되었습니다."),
    SERPAPI_GOOGLE_TRENDS_FAILED(HttpStatus.BAD_GATEWAY, "구글 트렌드 이슈 조회에 실패했습니다."),

    // Room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 토론방입니다."),
    ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 토픽으로 생성된 토론방이 존재합니다."),
    ROOM_CLOSED(HttpStatus.BAD_REQUEST, "이미 종료된 토론방입니다."),
    ROOM_NOT_OPEN(HttpStatus.BAD_REQUEST, "현재 입장 가능한 토론방이 아닙니다."),
    ROOM_FULL(HttpStatus.CONFLICT, "토론방 인원이 가득 찼습니다."),
    ROOM_ALREADY_PARTICIPATED(HttpStatus.CONFLICT, "이미 참여 중인 토론방입니다."),
    ROOM_PARTICIPATION_REQUIRED(HttpStatus.FORBIDDEN, "토론방에 참여 중인 사용자만 의견을 작성할 수 있습니다."),
    ROOM_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "토론방 참여 정보를 찾을 수 없습니다."),

    // Chat
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채팅 메시지입니다."),
    CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "채팅 메시지는 비어 있을 수 없습니다."),
    CHAT_MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "채팅 메시지 길이를 초과했습니다."),
    CHAT_MESSAGE_CONTAINS_PROFANITY(HttpStatus.BAD_REQUEST, "채팅 메시지에 금칙어가 포함되어 있습니다."),
    CHAT_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "채팅 메시지 전송 제한을 초과했습니다."),

    // Speaking Queue
    SPEAKING_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 발언권을 신청한 상태입니다."),
    SPEAKING_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "발언권 신청 내역이 존재하지 않습니다."),
    SPEAKING_REQUEST_NOT_CANCELABLE(HttpStatus.CONFLICT, "대기 중인 발언권 신청만 취소할 수 있습니다."),
    SPEAKING_QUEUE_ORDER_CONFLICT(HttpStatus.CONFLICT, "발언권 대기 순번 충돌이 발생했습니다. 다시 시도해주세요."),
    SPEAKING_QUEUE_EMPTY(HttpStatus.BAD_REQUEST, "발언권 대기열이 비어 있습니다."),
    CURRENT_SPEAKER_ALREADY_EXISTS(HttpStatus.CONFLICT, "현재 발언자가 이미 존재합니다."),
    CURRENT_SPEAKER_NOT_FOUND(HttpStatus.NOT_FOUND, "현재 발언자가 존재하지 않습니다."),
    SPEAKING_TIME_EXPIRED(HttpStatus.BAD_REQUEST, "발언 시간이 만료되었습니다."),

    // Speech
    SPEECH_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 발언입니다."),
    SPEECH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 발언에 접근할 권한이 없습니다."),
    SPEECH_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "수정할 수 없는 발언 상태입니다."),
    SPEECH_CONTENT_CONTAINS_PROFANITY(HttpStatus.BAD_REQUEST, "욕설 또는 비속어가 포함된 의견은 등록할 수 없습니다."),
    SPEECH_REPORT_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인의 의견은 신고할 수 없습니다."),
    SPEECH_REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신고한 의견입니다."),
    SPEECH_REPORT_DESCRIPTION_REQUIRED(HttpStatus.BAD_REQUEST, "기타 신고 사유에는 상세 설명이 필요합니다."),
    SPEECH_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 의견 신고입니다."),
    SPEECH_REPORT_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "현재 상태에서 요청한 신고 처리를 수행할 수 없습니다."),
    SPEECH_REPORT_RESOLUTION_NOTE_REQUIRED(HttpStatus.BAD_REQUEST, "신고 처리 완료 또는 반려 시 처리 사유가 필요합니다."),
    SPEECH_REPORT_RESOLUTION_NOTE_TOO_LONG(HttpStatus.BAD_REQUEST, "신고 처리 사유는 500자 이하여야 합니다."),
    SPEECH_REPORT_SEVERITY_REQUIRED(HttpStatus.BAD_REQUEST, "신고 처리 완료 시 위반 심각도가 필요합니다."),
    SPEECH_REPORT_SEVERITY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "검토 시작 또는 반려 처리에는 위반 심각도를 지정할 수 없습니다."),
    SPEECH_REACTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 공감한 의견입니다."),
    SPEECH_REACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "취소할 의견 공감이 존재하지 않습니다."),
    SPEECH_REACTION_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인의 의견에는 공감할 수 없습니다."),
    BEST_SPEECH_NOT_FOUND(HttpStatus.NOT_FOUND, "베스트 의견이 존재하지 않습니다."),
    INVALID_STANCE(HttpStatus.BAD_REQUEST, "올바르지 않은 입장 값입니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다."),

    // AI Report
    AI_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 리포트를 찾을 수 없습니다."),
    AI_REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 AI 리포트가 생성 중이거나 생성되었습니다."),
    AI_REPORT_ROOM_NOT_CLOSED(HttpStatus.BAD_REQUEST, "종료된 토론방만 AI 리포트를 생성할 수 있습니다."),
    AI_REPORT_CONFIG_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "AI 리포트 서버 설정이 누락되었습니다."),
    AI_REPORT_GENERATE_FAILED(HttpStatus.BAD_GATEWAY, "AI 리포트 생성에 실패했습니다."),
    AI_REPORT_QUEUE_PUBLISH_FAILED(HttpStatus.BAD_GATEWAY, "AI 리포트 생성 작업을 큐에 등록하지 못했습니다."),
    AI_REPORT_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI 리포트 서버 응답 형식이 올바르지 않습니다."),
    AI_REPORT_CUSTOM_PROMPT_TOO_MANY(HttpStatus.BAD_REQUEST, "customPrompts는 최대 5개까지 입력할 수 있습니다."),
    AI_REPORT_CUSTOM_PROMPT_REQUIRED(HttpStatus.BAD_REQUEST, "개인화 요청 prompt는 비어 있을 수 없습니다."),
    AI_REPORT_CUSTOM_PROMPT_TOO_LONG(HttpStatus.BAD_REQUEST, "개인화 요청 prompt는 정책 길이를 초과할 수 없습니다."),
    AI_REPORT_CUSTOM_PROMPT_INVALID(HttpStatus.BAD_REQUEST, "개인화 요청에 허용되지 않는 문자가 포함되어 있습니다."),
    PROMPT_GUARD_BLOCKED(HttpStatus.UNPROCESSABLE_ENTITY, "개인화 요청에 안전하지 않은 지시가 포함되어 있습니다."),
    PROMPT_GUARD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "개인화 요청 안전성 검사를 완료할 수 없습니다."),

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
