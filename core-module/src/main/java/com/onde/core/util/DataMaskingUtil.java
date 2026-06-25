package com.onde.core.util;

public class DataMaskingUtil {

    private DataMaskingUtil() {
        // Utility class
    }

    /**
     * 이메일 마스킹 (예: test@email.com -> te**@email.com)
     * 앞 2자리와 마지막 1자리를 제외하고 마스킹 처리합니다.
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String id = parts[0];
        String domain = parts[1];

        if (id.length() <= 3) {
            return id.charAt(0) + "**@" + domain;
        }
        
        StringBuilder maskedId = new StringBuilder();
        maskedId.append(id.substring(0, 2)); // 앞 2자리 노출
        for (int i = 2; i < id.length() - 1; i++) {
            maskedId.append("*");
        }
        maskedId.append(id.charAt(id.length() - 1)); // 마지막 1자리 노출

        return maskedId.toString() + "@" + domain;
    }

    /**
     * 이름 마스킹 (예: 홍길동 -> 홍*동, 김철 -> 김*)
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        
        StringBuilder maskedName = new StringBuilder();
        maskedName.append(name.charAt(0));
        for (int i = 1; i < name.length() - 1; i++) {
            maskedName.append("*");
        }
        maskedName.append(name.charAt(name.length() - 1));
        return maskedName.toString();
    }

    /**
     * 전화번호 마스킹 (예: 010-1234-5678 -> 010-****-5678 또는 01012345678 -> 010****5678)
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        
        if (phone.contains("-")) {
            String[] parts = phone.split("-");
            if (parts.length == 3) {
                String middle = parts[1];
                StringBuilder maskedMiddle = new StringBuilder();
                for (int i = 0; i < middle.length(); i++) {
                    maskedMiddle.append("*");
                }
                return parts[0] + "-" + maskedMiddle.toString() + "-" + parts[2];
            }
        } else {
            if (phone.length() >= 10 && phone.length() <= 11) {
                String prefix = phone.substring(0, 3);
                String suffix = phone.substring(phone.length() - 4);
                String middle = phone.substring(3, phone.length() - 4);
                
                StringBuilder maskedMiddle = new StringBuilder();
                for (int i = 0; i < middle.length(); i++) {
                    maskedMiddle.append("*");
                }
                return prefix + maskedMiddle.toString() + suffix;
            }
        }
        return phone;
    }

    /**
     * 사업자번호 마스킹 (예: 123-45-67890 -> 123-**-67890)
     */
    public static String maskBusinessNumber(String bizNum) {
        if (bizNum == null || bizNum.isEmpty()) {
            return bizNum;
        }
        if (bizNum.contains("-")) {
            String[] parts = bizNum.split("-");
            if (parts.length == 3) {
                return parts[0] + "-**-" + parts[2];
            }
        } else if (bizNum.length() == 10) {
            return bizNum.substring(0, 3) + "**" + bizNum.substring(5);
        }
        return bizNum;
    }

    public static String maskDate(String date) {
        if (date == null || date.isEmpty()) {
            return date;
        }
        if (date.contains("-")) {
            String[] parts = date.split("-");
            if (parts.length >= 3) {
                // 시간 정보가 포함된 경우 (예: 2023-05-12T10:00:00)
                if (parts[2].contains("T") || parts[2].contains(" ")) {
                     return parts[0] + "-" + parts[1] + "-**"; 
                }
                return parts[0] + "-" + parts[1] + "-**";
            }
        } else if (date.length() == 8 && date.matches("\\d{8}")) {
            // 하이픈 없는 YYYYMMDD 포맷 (예: 20230512 -> 202305**)
            return date.substring(0, 6) + "**";
        }
        return date;
    }
}
