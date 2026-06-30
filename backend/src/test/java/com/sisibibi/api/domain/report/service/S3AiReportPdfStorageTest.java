package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReportPdfType;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.storage.S3StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3AiReportPdfStorageTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3StorageProperties storageProperties;
    private AiReportPdfProperties pdfProperties;

    private S3AiReportPdfStorage storage;

    @BeforeEach
    void setUp() {
        storageProperties = new S3StorageProperties();
        storageProperties.setBucket("test-bucket");
        storageProperties.setRegion("ap-northeast-2");

        pdfProperties = new AiReportPdfProperties();

        storage = new S3AiReportPdfStorage(s3Client, s3Presigner, storageProperties, pdfProperties);
    }

    @Test
    void upload_putsPrivatePdfObjectWithStableKey() {
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        String key = storage.upload(1L, 10L, 7L, AiReportPdfType.BASE, new byte[]{1, 2, 3});

        assertThat(key).isEqualTo("ai-reports/1/10/7-base.pdf");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
    }

    @Test
    void createDownloadUrl_usesAttachmentDisposition() throws MalformedURLException {
        PresignedGetObjectRequest presignedRequest = presignedRequest("https://s3.example.com/presigned");
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presignedRequest);

        AiReportPdfStorage.DownloadUrl result = storage.createDownloadUrl("ai-reports/1/10/7.pdf", "ai-report-room-1.pdf");

        assertThat(result.url()).isEqualTo("https://s3.example.com/presigned");
        assertThat(result.expiresAt()).isNotNull();
    }

    @Test
    void missingS3Config_throwsCustomException() {
        storageProperties.setBucket(null);

        assertThatThrownBy(() -> storage.upload(1L, 10L, 7L, AiReportPdfType.BASE, new byte[]{1}))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.S3_CONFIG_MISSING);
    }

    private PresignedGetObjectRequest presignedRequest(String url) throws MalformedURLException {
        PresignedGetObjectRequest mock = org.mockito.Mockito.mock(PresignedGetObjectRequest.class,
                org.mockito.Mockito.withSettings().lenient());
        given(mock.url()).willReturn(new URL(url));
        return mock;
    }
}
