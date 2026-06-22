package com.sisibibi.api.domain.usersanction.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UserSanctionExtendReq(
        @NotNull(message = "제재 연장 기간은 필수입니다.")
        @Positive(message = "제재 연장 기간은 1시간 이상이어야 합니다.")
        @Max(value = 720, message = "제재 연장 기간은 최대 720시간입니다.")
        Integer durationHours,

        @NotBlank(message = "제재 연장 사유는 필수입니다.")
        @Size(max = 500, message = "제재 연장 사유는 500자 이하여야 합니다.")
        String reason
) {
}
