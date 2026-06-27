package com.onde.admin.application.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminMemberRevealResponse {
    private Long memberId;
    private String email;
    private String name;
}
