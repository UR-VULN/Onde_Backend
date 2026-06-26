package com.onde.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DB 저장 전 XSS 필터링에서 제외할 필드에 사용합니다.
 * 비밀번호 해시, 토큰, URL 등 인코딩 시 손상될 수 있는 값에 적용합니다.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipInputSanitization {
}
