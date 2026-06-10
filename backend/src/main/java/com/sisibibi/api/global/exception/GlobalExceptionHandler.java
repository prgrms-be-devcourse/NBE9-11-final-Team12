package com.sisibibi.api.global.exception;

import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String USERS_EMAIL_UNIQUE_CONSTRAINT = "uk_users_email";
    private static final String ROOMS_TOPIC_ID_UNIQUE_CONSTRAINT = "uk_rooms_topic_id";
    private static final String ROOM_PARTICIPANTS_ROOM_USER_UNIQUE_CONSTRAINT =
            "uk_room_participants_room_id_user_id";
    private static final String PAYMENTS_ORDER_ID_UNIQUE_CONSTRAINT = "uk_payments_order_id";

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("Business exception. code={}, message={}",
                errorCode.name(),
                errorCode.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(
                        errorCode.getStatus(),
                        errorCode.name(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        log.warn("Request body validation failed. errors={}", errors);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(
                        errorCode.getStatus(),
                        errorCode.name(),
                        errorCode.getMessage(),
                        errors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException e
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        e.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
            errors.putIfAbsent(fieldName, violation.getMessage());
        });

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        log.warn("Request parameter validation failed. errors={}", errors);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(
                        errorCode.getStatus(),
                        errorCode.name(),
                        errorCode.getMessage(),
                        errors
                ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e
    ) {
        ErrorCode errorCode = ErrorCode.FILE_SIZE_EXCEEDED;

        log.warn("File size exceeded.", e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(
                        errorCode.getStatus(),
                        errorCode.name(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException e
    ) {
        ErrorCode errorCode = resolveDataIntegrityViolationErrorCode(e);

        if (errorCode == ErrorCode.INTERNAL_SERVER_ERROR) {
            log.error("Unhandled data integrity violation.", e);
        } else {
            log.warn("Data integrity violation. code={}", errorCode.name());
        }

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(
                        errorCode.getStatus(),
                        errorCode.name(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        log.error("Unexpected server error.", e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(
                        errorCode.getStatus(),
                        errorCode.name(),
                        errorCode.getMessage()
                ));
    }

    private ErrorCode resolveDataIntegrityViolationErrorCode(DataIntegrityViolationException e) {
        if (isConstraintViolation(e, USERS_EMAIL_UNIQUE_CONSTRAINT)) {
            return ErrorCode.DUPLICATE_EMAIL;
        }

        if (isConstraintViolation(e, ROOMS_TOPIC_ID_UNIQUE_CONSTRAINT)) {
            return ErrorCode.ROOM_ALREADY_EXISTS;
        }

        if (isConstraintViolation(e, ROOM_PARTICIPANTS_ROOM_USER_UNIQUE_CONSTRAINT)) {
            return ErrorCode.ROOM_ALREADY_PARTICIPATED;
        }

        if (isConstraintViolation(e, PAYMENTS_ORDER_ID_UNIQUE_CONSTRAINT)) {
            return ErrorCode.DUPLICATE_PAYMENT;
        }

        return ErrorCode.INTERNAL_SERVER_ERROR;
    }

    private boolean isConstraintViolation(Throwable throwable, String expectedConstraintName) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException ex) {
                String actualConstraintName = ex.getConstraintName();
                return actualConstraintName != null
                        && actualConstraintName.endsWith(expectedConstraintName);
            }

            current = current.getCause();
        }

        return false;
    }
}