package com.onde.api.application.member.dto;

import lombok.Builder;
import lombok.Getter;

/** 본인 확인 후 클릭 시에만 반환하는 프로필 원문 (기본 profile API는 마스킹) */
@Getter
@Builder
public class MemberProfileRevealResponse {
    private String email;
    private String name;
    private String phoneNumber;
}
