package com.onde.admin.application.member.dto;

import com.onde.core.entity.member.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberAdminResponse {
    private Long memberId;
    private String email;
    private String name;
    private String role;
    private String status;
    private String provider;
    private LocalDateTime createdAt;

    // Entity -> DTO 변환을 위한 편의 메서드
    public static MemberAdminResponse from(Member member) {
        return MemberAdminResponse.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .role(member.getRole() != null ? member.getRole().name() : null)
                .status(member.getStatus() != null ? member.getStatus().name() : null)
                .provider(member.getProvider() != null ? member.getProvider().name() : null)
                .createdAt(member.getCreatedAt())
                .build();
    }
}
