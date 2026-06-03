package com.onde.admin.application.member.dto;

import com.onde.core.entity.member.MemberStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberStatusUpdateRequest {
    private MemberStatus status;
}
