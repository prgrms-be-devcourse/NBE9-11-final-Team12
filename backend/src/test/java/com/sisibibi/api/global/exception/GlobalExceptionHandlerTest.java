package com.sisibibi.api.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.sisibibi.api.global.response.ApiResponse;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrityViolationException_mapsKnownConstraintNames() {
        assertConstraint("schema.uk_users_email", ErrorCode.DUPLICATE_EMAIL);
        assertConstraint("schema.uk_rooms_topic_id", ErrorCode.ROOM_ALREADY_EXISTS);
        assertConstraint("schema.uk_room_participants_room_id_user_id", ErrorCode.ROOM_ALREADY_PARTICIPATED);
        assertConstraint("schema.uk_speaking_queue_room_order", ErrorCode.SPEAKING_QUEUE_ORDER_CONFLICT);
        assertConstraint("schema.uk_speaking_queue_room_user_active", ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
        assertConstraint("schema.uk_speech_reports_speech_reporter", ErrorCode.SPEECH_REPORT_ALREADY_EXISTS);
        assertConstraint("schema.uk_speech_reactions_speech_user", ErrorCode.SPEECH_REACTION_ALREADY_EXISTS);
        assertConstraint("schema.uk_payments_order_id", ErrorCode.DUPLICATE_PAYMENT);
    }

    @Test
    void handleDataIntegrityViolationException_returnsInternalError_whenConstraintIsUnknown() {
        DataIntegrityViolationException exception = constraintViolation("unknown_constraint");

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.name());
    }

    @Test
    void handleDataIntegrityViolationException_returnsInternalError_whenConstraintNameIsNull() {
        DataIntegrityViolationException exception = constraintViolation(null);

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.name());
    }

    @Test
    void handleDataIntegrityViolationException_returnsInternalError_whenCauseIsNotHibernateConstraint() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "integrity violation",
                new IllegalStateException("not hibernate")
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.name());
    }

    @Test
    void handleCustomException_returnsBusinessErrorResponse() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleCustomException(new CustomException(ErrorCode.USER_NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.name());
    }

    @Test
    void handleException_returnsInternalServerError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(new RuntimeException("unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.name());
    }

    private void assertConstraint(String constraintName, ErrorCode expectedErrorCode) {
        DataIntegrityViolationException exception = constraintViolation(constraintName);

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(expectedErrorCode.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(expectedErrorCode.name());
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "integrity violation",
                new org.hibernate.exception.ConstraintViolationException(
                        "constraint violation",
                        new SQLException("constraint violation"),
                        constraintName
                )
        );
    }
}
