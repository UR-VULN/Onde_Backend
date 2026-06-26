package com.onde.api.utils;

public class MaskingUtils {

    /**
     * 이름 마스킹 (예: 홍길동 -> 홍*동, 남궁길동 -> 남**동, Lee -> L*e)
     */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        name = name.trim();
        int len = name.length();
        if (len <= 1) {
            return name;
        }
        if (len == 2) {
            return name.charAt(0) + "*";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(name.charAt(0));
        for (int i = 1; i < len - 1; i++) {
            sb.append("*");
        }
        sb.append(name.charAt(len - 1));
        return sb.toString();
    }

    /**
     * 예약 코드/고유 번호 마스킹 (중간 문자 마스킹: 예: BK-20260626-A1B2C3D4 -> BK-20******-A1B2C3D4)
     */
    public static String maskMiddle(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        code = code.trim();
        int len = code.length();
        if (len <= 8) {
            return code.replaceAll(".", "*");
        }
        
        int keepStart = 5;
        int keepEnd = 5;
        if (len <= keepStart + keepEnd) {
            keepStart = len / 2;
            keepEnd = len - keepStart;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(code.substring(0, keepStart));
        sb.append("*".repeat(len - keepStart - keepEnd));
        sb.append(code.substring(len - keepEnd));
        return sb.toString();
    }
}
