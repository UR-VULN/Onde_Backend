package com.onde.core.util;

/**
 * [보안 강화 - WEB-5 / WEB-8] 개인정보 노출 최소화를 위한 전역 마스킹 유틸리티
 */
public class MaskingUtil {

    /**
     * 이메일 주소를 마스킹 처리합니다.
     * 예: sensitive@travel.com -> se*******@travel.com
     *     ab@test.com -> a*@test.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        if (!email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String id = parts[0];
        String domain = parts[1];

        if (id.length() <= 2) {
            if (id.isEmpty()) return email;
            return id.charAt(0) + "*" + "@" + domain;
        }

        String visible = id.substring(0, 2);
        String masked = "*".repeat(id.length() - 2);
        return visible + masked + "@" + domain;
    }

    /**
     * 계좌번호를 마스킹 처리합니다.
     * 예: 123-456-7890 -> 123-456-****
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "";
        }
        String clean = accountNumber.replaceAll("[^0-9-]", "");
        if (clean.length() < 4) {
            return "****";
        }
        return clean.substring(0, clean.length() - 4) + "****";
    }
}
