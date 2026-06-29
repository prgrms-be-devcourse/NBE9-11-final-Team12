package com.sisibibi.api.domain.report.notification;

import com.sisibibi.api.domain.report.service.AiReportNotificationSender;
import com.sisibibi.api.domain.report.service.AiReportPdfReadyCommand;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ai-report.notification", name = "provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpAiReportNotificationSender implements AiReportNotificationSender {

    private final JavaMailSender mailSender;
    private final AiReportNotificationProperties properties;

    @Override
    public void sendPdfReady(AiReportPdfReadyCommand command) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(properties.getFromEmail());
            helper.setTo(command.recipientEmail());
            helper.setSubject("AI 토론 리포트 PDF가 준비되었습니다");
            helper.setText("""
                    %s님, AI 토론 리포트 PDF가 준비되었습니다.

                    토론방: %s
                    홈페이지에서 로그인한 뒤 다운로드 버튼을 눌러 PDF를 내려받을 수 있습니다.
                    %s
                    """.formatted(command.recipientNickname(), command.roomTitle(), command.homepageUrl()), false);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new IllegalStateException("Failed to send AI report PDF ready email.", e);
        }
    }
}
