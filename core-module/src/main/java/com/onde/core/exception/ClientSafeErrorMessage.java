package com.onde.core.exception;

import java.util.regex.Pattern;

/** API 클라이언트에 노출할 수 있는 메시지인지 판별합니다. */
public final class ClientSafeErrorMessage {

    private static final int MAX_CLIENT_MESSAGE_LENGTH = 200;

    private static final Pattern[] TECHNICAL_PATTERNS = {
            Pattern.compile("(?i)exception"),
            Pattern.compile("java\\."),
            Pattern.compile("org\\.springframework"),
            Pattern.compile("org\\.hibernate"),
            Pattern.compile("(?i)jdbc"),
            Pattern.compile("(?i)sql"),
            Pattern.compile("(?i)propertykey"),
            Pattern.compile("(?i)failed:"),
            Pattern.compile("(?i)^unknown "),
            Pattern.compile("(?i)generation failed"),
            Pattern.compile("(?i)nullpointer"),
            Pattern.compile("(?i)stacktrace"),
    };

    private ClientSafeErrorMessage() {
    }

    public static String fromIllegalArgument(IllegalArgumentException e) {
        return sanitize(e.getMessage(), "입력 형식이 올바르지 않습니다.");
    }

    public static String sanitize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_CLIENT_MESSAGE_LENGTH || looksTechnical(trimmed)) {
            return fallback;
        }
        return trimmed;
    }

    private static boolean looksTechnical(String message) {
        for (Pattern pattern : TECHNICAL_PATTERNS) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }
        return !containsHangul(message) && message.matches(".*[A-Za-z]{4,}.*");
    }

    private static boolean containsHangul(String message) {
        for (int i = 0; i < message.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(message.charAt(i));
            if (block == Character.UnicodeBlock.HANGUL_SYLLABLES
                    || block == Character.UnicodeBlock.HANGUL_JAMO
                    || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
                return true;
            }
        }
        return false;
    }
}
