package com.onde.admin.application.member.dto;

import com.onde.core.entity.member.Member;
import com.onde.core.security.PersonalDataMasker;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberAdminResponse {
    /** 관리자 조작용 내부 식별자 (목록 API) */
    private Long id;
    /** 화면 표시용 마스킹 memberId */
    private String memberId;
    private String email;
    private String name;
    private String role;
    private String status;
    private String provider;
    private LocalDateTime createdAt;

    public static MemberAdminResponse from(Member member) {
        Long id = member.getId();
        return MemberAdminResponse.builder()
                .id(id)
                .memberId(PersonalDataMasker.maskNumericId(id))
                .email(PersonalDataMasker.maskEmail(member.getEmail()))
                .name(PersonalDataMasker.maskName(member.getName()))
                .role(member.getRole() != null ? member.getRole().name() : null)
                .status(member.getStatus() != null ? member.getStatus().name() : null)
                .provider(member.getProvider() != null ? member.getProvider().name() : null)
                .createdAt(member.getCreatedAt())
                .build();
    }
}
