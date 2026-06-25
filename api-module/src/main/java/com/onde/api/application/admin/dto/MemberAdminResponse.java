package com.onde.api.application.admin.dto;

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

    public static MemberAdminResponse from(Member member) {
        return MemberAdminResponse.builder()
                .memberId(member.getId())
                .email(maskEmail(member.getEmail()))
                .name(maskName(member.getName() != null ? member.getName() : member.getEmail()))
                .role(member.getRole() != null ? member.getRole().name() : null)
                .status(member.getStatus() != null ? member.getStatus().name() : null)
                .provider(member.getProvider() != null ? member.getProvider().name() : null)
                .createdAt(member.getCreatedAt())
                .build();
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String id = parts[0];
        String domain = parts[1];
        if (id.length() <= 2) {
            return id + "@" + domain;
        }
        return id.substring(0, 2) + "*".repeat(id.length() - 2) + "@" + domain;
    }

    private static String maskName(String name) {
        if (name == null || name.length() <= 2) {
            return name;
        }
        return name.substring(0, 2) + "*".repeat(name.length() - 2);
    }
}
