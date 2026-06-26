package com.onde.core.validation;

/**
 * 패스워드 복잡도 정책 수준 (가이드라인 3-1).
 */
public enum PasswordPolicyLevel {
    /** 일반 회원 — 2종 조합 10자+ / 3종 조합 8자+ */
    USER,
    /** 관리자 — 대·소문자·숫자·특수문자 모두 + 10자+ */
    ADMIN
}
