package com.onde.api.application.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberMeResponse {
    private Long memberId;
    private String email;
    private String role;
    private String status;
}