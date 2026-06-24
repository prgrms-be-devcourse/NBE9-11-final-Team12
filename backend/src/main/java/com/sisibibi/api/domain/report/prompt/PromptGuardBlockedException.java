package com.sisibibi.api.domain.report.prompt;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class PromptGuardBlockedException extends CustomException {

    private final PromptSeverity severity;

    public PromptGuardBlockedException(PromptSeverity severity) {
        super(ErrorCode.PROMPT_GUARD_BLOCKED);
        this.severity = severity == null ? PromptSeverity.HIGH : severity;
    }
}
