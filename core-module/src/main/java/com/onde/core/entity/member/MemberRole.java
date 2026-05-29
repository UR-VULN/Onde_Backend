package com.onde.core.entity.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {
    GUEST("ROLE_GUEST", "임시 가입자"),
    USER("ROLE_USER", "일반 사용자"),
    SELLER("ROLE_SELLER", "판매자"),
    GENERAL_ADMIN("ROLE_GENERAL_ADMIN", "일반 관리자"),
    SUPER_ADMIN("ROLE_SUPER_ADMIN", "최고 관리자"),
    BLACKLIST("ROLE_BLACKLIST", "블랙리스트");

    private final String securityRole; // Spring Security용 권한 명칭
    private final String description; // 부가 설명
}