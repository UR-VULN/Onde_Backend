package com.onde.core.security;

/**
 * 일반 사용자·판매자 화면/API 응답용 개인정보 마스킹.
 */
public final class PersonalDataMasker {

    private PersonalDataMasker() {
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        String maskedLocal = local.length() <= 2
                ? local.charAt(0) + "***"
                : local.substring(0, 2) + "***";
        return maskedLocal + "@" + maskDomain(domain);
    }

    private static String maskDomain(String domain) {
        int dot = domain.lastIndexOf('.');
        if (dot <= 0) {
            return "***";
        }
        String name = domain.substring(0, dot);
        String tld = domain.substring(dot);
        if (name.length() <= 2) {
            return "**" + tld;
        }
        return name.substring(0, 2) + "***" + tld;
    }

    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String trimmed = name.trim();
        int length = trimmed.length();
        if (length == 1) {
            return "*";
        }
        if (length == 2) {
            return trimmed.charAt(0) + "*";
        }
        return trimmed.charAt(0) + "*".repeat(length - 2) + trimmed.charAt(length - 1);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 8) {
            return "***";
        }
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-***-" + digits.substring(6);
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "";
        }
        String trimmed = accountNumber.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        if (trimmed.contains("-")) {
            String[] parts = trimmed.split("-");
            if (parts.length >= 3) {
                StringBuilder sb = new StringBuilder();
                sb.append(parts[0]).append("-");
                for (int i = 1; i < parts.length - 1; i++) {
                    sb.append("*".repeat(parts[i].length())).append("-");
                }
                sb.append(parts[parts.length - 1]);
                return sb.toString();
            }
        }
        if (trimmed.length() < 8) {
            return "****";
        }
        return trimmed.substring(0, 3) + "-***-" + trimmed.substring(trimmed.length() - 4);
    }

    public static String maskBankName(String bankName) {
        if (bankName == null || bankName.isBlank()) {
            return "";
        }
        String trimmed = bankName.trim();
        if (trimmed.length() <= 2) {
            return trimmed.charAt(0) + "*";
        }
        return trimmed.substring(0, 2) + "***";
    }

    public static String maskPassport(String passport) {
        if (passport == null || passport.isBlank()) {
            return "";
        }
        String trimmed = passport.trim();
        int length = trimmed.length();
        if (length <= 2) {
            return "*".repeat(length);
        }
        if (length <= 4) {
            return trimmed.charAt(0) + "*".repeat(length - 2) + trimmed.charAt(length - 1);
        }
        return trimmed.substring(0, 2) + "*".repeat(length - 4) + trimmed.substring(length - 2);
    }
}
