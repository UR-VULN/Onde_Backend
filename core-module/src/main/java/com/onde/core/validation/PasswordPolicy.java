package com.onde.core.validation;

import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ValidationException;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * SK Shieldus 가이드라인 3-1 패스워드 복잡도·만료 규칙.
 */
public final class PasswordPolicy {

    public static final int USER_EXPIRY_DAYS = 60;
    public static final int ADMIN_EXPIRY_DAYS = 180;

    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "password", "password1", "password12", "password123",
            "12345678", "123456789", "qwerty123", "qwertyuiop",
            "abcde123", "admin123", "admin1234", "onde1234",
            "11111111", "aaaaaaaa", "abcd1234");

    private PasswordPolicy() {
    }

    public static void validateOrThrow(String password, PasswordPolicyLevel level) {
        validate(password, level).ifPresent(message -> {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, message);
        });
    }

    public static Optional<String> validate(String password, PasswordPolicyLevel level) {
        if (password == null || password.isBlank()) {
            return Optional.of("비밀번호는 필수 입력값입니다.");
        }
        if (password.length() > ValidationLimits.PASSWORD_MAX) {
            return Optional.of("비밀번호는 " + ValidationLimits.PASSWORD_MAX + "자 이하여야 합니다.");
        }
        if (containsWhitespace(password)) {
            return Optional.of("비밀번호에 공백을 포함할 수 없습니다.");
        }
        if (!isAllowedCharset(password)) {
            return Optional.of("비밀번호에 허용되지 않은 문자가 포함되어 있습니다.");
        }
        if (WEAK_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            return Optional.of("사용할 수 없는 비밀번호입니다. 더 강력한 비밀번호를 설정해 주세요.");
        }

        int categories = countCategories(password);
        if (level == PasswordPolicyLevel.ADMIN) {
            if (categories < 4) {
                return Optional.of("관리자 비밀번호는 영문 대·소문자, 숫자, 특수문자를 모두 포함해야 합니다.");
            }
            if (password.length() < 10) {
                return Optional.of("관리자 비밀번호는 10자 이상이어야 합니다.");
            }
            return Optional.empty();
        }

        if (categories < 2) {
            return Optional.of("비밀번호는 영문, 숫자, 특수문자 중 2가지 이상을 조합해야 합니다.");
        }
        if (categories >= 3) {
            if (password.length() < ValidationLimits.PASSWORD_MIN) {
                return Optional.of("3가지 이상 조합 시 비밀번호는 8자 이상이어야 합니다.");
            }
        } else if (password.length() < 10) {
            return Optional.of("2가지 조합 시 비밀번호는 10자 이상이어야 합니다.");
        }
        return Optional.empty();
    }

    public static int countCategories(String password) {
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                upper = true;
            } else if (Character.isLowerCase(ch)) {
                lower = true;
            } else if (Character.isDigit(ch)) {
                digit = true;
            } else {
                special = true;
            }
        }
        int count = 0;
        if (upper) count++;
        if (lower) count++;
        if (digit) count++;
        if (special) count++;
        return count;
    }

    private static boolean containsWhitespace(String password) {
        for (int i = 0; i < password.length(); i++) {
            if (Character.isWhitespace(password.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowedCharset(String password) {
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (ch < 33 || ch > 126) {
                return false;
            }
        }
        return true;
    }
}
