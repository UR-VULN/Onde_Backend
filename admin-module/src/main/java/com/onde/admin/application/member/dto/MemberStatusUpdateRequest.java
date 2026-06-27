package com.onde.admin.application.member.dto;

import com.onde.core.entity.member.MemberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberStatusUpdateRequest {

    @NotNull(message = "변경할 회원 상태(status)는 필수입니다.")
    private MemberStatus status;
}
