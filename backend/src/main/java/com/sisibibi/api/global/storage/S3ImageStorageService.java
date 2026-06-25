package com.sisibibi.api.global.storage;

import com.sisibibi.api.domain.speech.dto.response.SpeechImageUploadUrlRes;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3ImageStorageService {

  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/webp",
      "image/gif"
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

    return new SpeechImageUploadUrlRes(
        s3Presigner.presignPutObject(presignRequest).url().toString(),
        publicUrl(imageKey),
        imageKey,
        expiresAt
    );
  }

  public String resolveUploadedImageUrl(String imageKey) {
    try {
      s3Client.headObject(HeadObjectRequest.builder()
          .bucket(properties.getBucket())
          .key(imageKey)
          .build());
      return publicUrl(imageKey);
    } catch (NoSuchKeyException e) {
      throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
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