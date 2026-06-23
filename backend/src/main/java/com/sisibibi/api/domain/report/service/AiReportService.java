package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.AiReportClient;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.prompt.CustomPromptCommand;
import com.sisibibi.api.domain.report.prompt.PromptGuardBlockedException;
import com.sisibibi.api.domain.report.prompt.PromptGuardProperties;
import com.sisibibi.api.domain.report.prompt.PromptGuardResult;
import com.sisibibi.api.domain.report.prompt.PromptGuardService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportService {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final int MAX_LABEL_LENGTH = 50;

    private final AiReportPersistenceService aiReportPersistenceService;
    private final AiReportClient aiReportClient;
    private final PromptGuardService promptGuardService;
    private final PromptGuardProperties promptGuardProperties;

    public AiReportRes generateReport(Long roomId) {
        return generateReport(roomId, AiReportGenerateReq.empty());
    }

    public AiReportRes generateReport(Long roomId, AiReportGenerateReq request) {
        List<CustomPromptCommand> customPrompts = normalizeAndScanCustomPrompts(request);
        AiReportGenerationContext context = aiReportPersistenceService.prepareGeneration(roomId, customPrompts);

        if (!context.shouldCallAi()) {
            return context.response();
        }

        try {
            AiReportGenerateRes response = aiReportClient.generate(context.request());

            if (!response.hasRequiredFields()) {
                return aiReportPersistenceService.fail(
                        context.reportId(),
                        ErrorCode.AI_REPORT_INVALID_RESPONSE.getMessage()
                );
            }

            return aiReportPersistenceService.complete(context.reportId(), response);
        } catch (CustomException e) {
            return aiReportPersistenceService.fail(context.reportId(), e.getErrorCode().getMessage());
        } catch (RuntimeException e) {
            return aiReportPersistenceService.fail(
                    context.reportId(),
                    ErrorCode.AI_REPORT_GENERATE_FAILED.getMessage()
            );
        }
    }

    public AiReportRes getReport(Long roomId) {
        return aiReportPersistenceService.getReport(roomId);
    }

    private List<CustomPromptCommand> normalizeAndScanCustomPrompts(AiReportGenerateReq request) {
        List<AiReportGenerateReq.CustomPromptReq> prompts = request == null ? List.of() : request.customPrompts();

        if (prompts.size() > promptGuardProperties.getCustomPromptMaxCount()) {
            throw new CustomException(ErrorCode.AI_REPORT_CUSTOM_PROMPT_TOO_MANY);
        }

        List<CustomPromptCommand> normalizedPrompts = new java.util.ArrayList<>();
        for (int i = 0; i < prompts.size(); i++) {
            AiReportGenerateReq.CustomPromptReq promptReq = prompts.get(i);
            String prompt = normalizePrompt(promptReq == null ? null : promptReq.prompt());
            String label = normalizeLabel(promptReq == null ? null : promptReq.label(), i + 1);

            scanPrompt(prompt);
            normalizedPrompts.add(new CustomPromptCommand(label, prompt));
        }

        return List.copyOf(normalizedPrompts);
    }

    private String normalizePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new CustomException(ErrorCode.AI_REPORT_CUSTOM_PROMPT_REQUIRED);
        }

        String normalized = normalizeText(prompt);
        if (normalized.isBlank()) {
            throw new CustomException(ErrorCode.AI_REPORT_CUSTOM_PROMPT_REQUIRED);
        }

        if (normalized.length() > promptGuardProperties.getCustomPromptMaxLength()) {
            throw new CustomException(ErrorCode.AI_REPORT_CUSTOM_PROMPT_TOO_LONG);
        }

        return normalized;
    }

    private String normalizeLabel(String label, int index) {
        if (label == null || label.isBlank()) {
            return "custom " + index;
        }

        String normalized = normalizeText(label);
        if (normalized.isBlank()) {
            return "custom " + index;
        }

        return normalized.length() > MAX_LABEL_LENGTH
                ? normalized.substring(0, MAX_LABEL_LENGTH)
                : normalized;
    }

    private String normalizeText(String text) {
        if (hasInvalidUnicode(text) || hasDisallowedControlCharacter(text)) {
            throw new CustomException(ErrorCode.AI_REPORT_CUSTOM_PROMPT_INVALID);
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).trim();
        return WHITESPACE_PATTERN.matcher(normalized).replaceAll(" ");
    }

    private boolean hasInvalidUnicode(String text) {
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isHighSurrogate(current)) {
                if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1))) {
                    return true;
                }
                i++;
                continue;
            }

            if (Character.isLowSurrogate(current)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasDisallowedControlCharacter(String text) {
        return text.chars()
                .anyMatch(ch -> Character.isISOControl(ch) && ch != '\n' && ch != '\r' && ch != '\t');
    }

    private void scanPrompt(String prompt) {
        PromptGuardResult result;
        try {
            result = promptGuardService.scan(prompt);
        } catch (RuntimeException e) {
            if (promptGuardProperties.isFailOpen()) {
                log.warn("Prompt guard unavailable. failOpen=true, promptHash={}", hashPrompt(prompt));
                return;
            }

            throw e instanceof CustomException
                    ? e
                    : new CustomException(ErrorCode.PROMPT_GUARD_UNAVAILABLE);
        }

        if (result == null) {
            throw new CustomException(ErrorCode.PROMPT_GUARD_UNAVAILABLE);
        }

        if (result.blocked() || result.severity().blocksRequest()) {
            log.warn("Prompt guard blocked custom prompt. severity={}, reason={}, promptHash={}",
                    result.severity(),
                    result.reason(),
                    hashPrompt(prompt)
            );
            throw new PromptGuardBlockedException(result.severity());
        }

        if (result.severity().requiresAuditLog()) {
            log.warn("Prompt guard audit for allowed custom prompt. severity={}, reason={}, promptHash={}",
                    result.severity(),
                    result.reason(),
                    hashPrompt(prompt)
            );
        }
    }

    private String hashPrompt(String prompt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(prompt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
