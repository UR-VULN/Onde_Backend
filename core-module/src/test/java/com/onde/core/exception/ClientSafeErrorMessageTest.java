package com.onde.core.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientSafeErrorMessageTest {

    @Test
    void sanitize_allowsKoreanUserMessage() {
        assertEquals(
                "본인의 정산 내역만 조회할 수 있습니다.",
                ClientSafeErrorMessage.sanitize("본인의 정산 내역만 조회할 수 있습니다.", "fallback"));
    }

    @Test
    void sanitize_blocksTechnicalEnglishMessage() {
        assertEquals(
                "입력 형식이 올바르지 않습니다.",
                ClientSafeErrorMessage.fromIllegalArgument(
                        new IllegalArgumentException("Unknown propertyKey prefix: stay-1")));
    }

    @Test
    void sanitize_blocksExceptionClassNames() {
        assertEquals(
                "fallback",
                ClientSafeErrorMessage.sanitize("java.lang.NullPointerException: detail", "fallback"));
    }
}
