package com.onde.api.application.auth.dto;

import com.onde.core.entity.member.MemberRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SignupResponse {
    private Long memberId;
    private String email;
    private String name;
    private MemberRole role;
    private LocalDateTime createdAt;
}
