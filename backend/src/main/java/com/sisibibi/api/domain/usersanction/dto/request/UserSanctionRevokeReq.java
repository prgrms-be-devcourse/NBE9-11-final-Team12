package com.sisibibi.api.domain.usersanction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSanctionRevokeReq(
        @NotBlank(message = "제재 해제 사유는 필수입니다.")
        @Size(max = 500, message = "제재 해제 사유는 500자 이하여야 합니다.")
        String reason
) {
}
