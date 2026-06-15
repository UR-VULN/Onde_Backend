package com.onde.api.application.auth.dto;

import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
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
    private MemberStatus status;
    private String nickname;
    private Integer age;
    private LocalDateTime createdAt;
}
