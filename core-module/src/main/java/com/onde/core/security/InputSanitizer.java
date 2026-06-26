package com.onde.core.security;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

/**
 * SK Shieldus Web/API 개발보안 Guideline 기준 XSS 입력값 정제 유틸리티.
 * 위험 스크립트 패턴 제거 후 HTML 특수문자를 인코딩합니다.
 */
public final class InputSanitizer {

    private static final Pattern SCRIPT_TAG = Pattern.compile(
            "<\\s*script[^>]*>.*?</\\s*script\\s*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern STYLE_TAG = Pattern.compile(
            "<\\s*style[^>]*>.*?</\\s*style\\s*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern DANGEROUS_PROTOCOL = Pattern.compile(
            "javascript\\s*:",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern VBSCRIPT_PROTOCOL = Pattern.compile(
            "vbscript\\s*:",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATA_HTML_PROTOCOL = Pattern.compile(
            "data\\s*:\\s*text/html",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EVENT_HANDLER = Pattern.compile(
            "\\bon\\w+\\s*=",
            Pattern.CASE_INSENSITIVE
    );

    private InputSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String cleaned = value;
        cleaned = SCRIPT_TAG.matcher(cleaned).replaceAll("");
        cleaned = STYLE_TAG.matcher(cleaned).replaceAll("");
        cleaned = DANGEROUS_PROTOCOL.matcher(cleaned).replaceAll("");
        cleaned = VBSCRIPT_PROTOCOL.matcher(cleaned).replaceAll("");
        cleaned = DATA_HTML_PROTOCOL.matcher(cleaned).replaceAll("");
        cleaned = EVENT_HANDLER.matcher(cleaned).replaceAll("");

        return HtmlUtils.htmlEscape(cleaned == null ? "" : cleaned);
    }
}
