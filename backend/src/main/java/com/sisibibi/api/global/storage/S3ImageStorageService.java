package com.sisibibi.api.global.storage;

import com.sisibibi.api.domain.speech.dto.response.SpeechImageUploadUrlRes;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageStorageService {

  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/webp"
  );

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final S3StorageProperties properties;

  public SpeechImageUploadUrlRes createSpeechImageUploadUrl(
      Long speechId,
      Long userId,
      String contentType,
      long fileSize
  ) {
    validateS3Config();
    validateImage(contentType, fileSize);

    String imageKey = "speeches/%d/%d/%s%s".formatted(
        speechId,
        userId,
        UUID.randomUUID(),
        extensionOf(contentType)
    );

    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(properties.getBucket())
        .key(imageKey)
        .contentType(contentType)
        .contentLength(fileSize)
        .build();

    Instant expiresAt = Instant.now().plus(properties.getPresignedUrlExpiration());

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(properties.getPresignedUrlExpiration())
        .putObjectRequest(putObjectRequest)
        .build();

    try {
      return new SpeechImageUploadUrlRes(
          s3Presigner.presignPutObject(presignRequest).url().toString(),
          publicUrl(imageKey),
          imageKey,
          expiresAt
      );
    } catch (SdkClientException | S3Exception e) {
      logS3Failure("create presigned upload URL", imageKey, e);
      throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
    }
  }

  public String resolveUploadedImageUrl(Long speechId, Long userId, String imageKey) {
    validateS3Config();
    validateSpeechImageKeyOwner(speechId, userId, imageKey);

    try {
      s3Client.headObject(HeadObjectRequest.builder()
          .bucket(properties.getBucket())
          .key(imageKey)
          .build());
      return publicUrl(imageKey);
    } catch (NoSuchKeyException e) {
      log.warn("S3 object not found. imageKey={}", imageKey);
      throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
    } catch (S3Exception e) {
      logS3Failure("check uploaded image", imageKey, e);
      throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
    } catch (SdkClientException e) {
      logS3Failure("check uploaded image", imageKey, e);
      throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
    }
  }
  public void deleteObjectQuietly(String imageKey) {
    if (!StringUtils.hasText(imageKey)) {
      return;
    }

    try {
      s3Client.deleteObject(DeleteObjectRequest.builder()
          .bucket(properties.getBucket())
          .key(imageKey)
          .build());
    } catch (S3Exception e) {
      log.warn(
          "Failed to delete orphan S3 object. imageKey={}, statusCode={}",
          imageKey,
          e.statusCode(),
          e
      );
    } catch (SdkClientException e) {
      log.warn("Failed to delete orphan S3 object. imageKey={}", imageKey, e);
    }
  }

  private void validateImage(String contentType, long fileSize) {
    if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
      throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
    }

    if (fileSize > properties.getMaxImageSize()) {
      throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
    }
  }

  private void validateS3Config() {
    if (!StringUtils.hasText(properties.getBucket())
        || !StringUtils.hasText(properties.getRegion())
        || !StringUtils.hasText(properties.getPublicBaseUrl())) {
      throw new CustomException(ErrorCode.S3_CONFIG_MISSING);
    }
  }

  private void validateSpeechImageKeyOwner(Long speechId, Long userId, String imageKey) {
    String expectedPrefix = "speeches/%d/%d/".formatted(speechId, userId);

    if (!StringUtils.hasText(imageKey) || !imageKey.startsWith(expectedPrefix)) {
      throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
    }
  }

  private void logS3Failure(String action, String imageKey, RuntimeException e) {
    if (e instanceof S3Exception s3Exception) {
      log.warn(
          "S3 image operation failed. action={}, imageKey={}, statusCode={}",
          action,
          imageKey,
          s3Exception.statusCode(),
          s3Exception
      );
      return;
    }

    log.warn("S3 image operation failed. action={}, imageKey={}", action, imageKey, e);
  }

  private String publicUrl(String imageKey) {
    return properties.getPublicBaseUrl().replaceAll("/$", "") + "/" + imageKey;
  }

  private String extensionOf(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      case "image/gif" -> ".gif";
      default -> throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
    };
  }
}