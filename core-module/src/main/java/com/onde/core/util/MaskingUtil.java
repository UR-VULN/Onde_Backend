package com.onde.core.util;

public class MaskingUtil {

    /**
     * 전화번호 마스킹 (010-1234-5678 -> 010-****-5678)
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        
        // 010-XXXX-XXXX 형식 검사
        String[] parts = phone.split("-");
        if (parts.length == 3) {
            return parts[0] + "-****-" + parts[2];
        }
        
        // 형식이 다를 경우 앞 3자리, 뒤 4자리 제외하고 모두 * 처리
        if (phone.length() >= 8) {
            String prefix = phone.substring(0, 3);
            String suffix = phone.substring(phone.length() - 4);
            return prefix + "*".repeat(phone.length() - 7) + suffix;
        }
        
        return phone;
    }

    /**
     * 이메일 마스킹 (jiho@travel.com -> ji**@travel.com)
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String id = parts[0];
        String domain = parts[1];

        if (id.length() <= 2) {
            return id.charAt(0) + "*@" + domain;
        }

        int maskLen = id.length() / 2;
        int visibleLen = id.length() - maskLen;
        
        String visible = id.substring(0, visibleLen);
        String masked = "*".repeat(maskLen);

        return visible + masked + "@" + domain;
    }
}
