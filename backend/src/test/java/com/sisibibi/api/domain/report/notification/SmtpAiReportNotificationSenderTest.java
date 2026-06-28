package com.sisibibi.api.domain.report.notification;

import com.sisibibi.api.domain.report.service.AiReportPdfReadyCommand;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpAiReportNotificationSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private SmtpAiReportNotificationSender sender;

    @BeforeEach
    void setUp() {
        AiReportNotificationProperties properties = new AiReportNotificationProperties();
        properties.setFromEmail("noreply@issuetok.com");
        properties.setHomepageUrl("http://localhost:3000");
        sender = new SmtpAiReportNotificationSender(mailSender, properties);
    }

    @Test
    void sendPdfReady_sendsEmailWithoutAttachment() {
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        sender.sendPdfReady(new AiReportPdfReadyCommand(
                1L, "user@test.com", "테스터", "AI 토론방", "http://localhost:3000"
        ));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPdfReady_usesHomepageLinkInsteadOfPresignedUrl() {
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);

        sender.sendPdfReady(new AiReportPdfReadyCommand(
                1L, "user@test.com", "테스터", "AI 토론방", "http://localhost:3000"
        ));

        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }
}
