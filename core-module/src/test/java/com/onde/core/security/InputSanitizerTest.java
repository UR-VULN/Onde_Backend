package com.onde.core.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InputSanitizerTest {

    @Test
    void sanitize_nullAndEmpty_areReturnedAsIs() {
        assertNull(InputSanitizer.sanitize(null));
        assertEquals("", InputSanitizer.sanitize(""));
    }

    @Test
    void sanitize_removesScriptTagsAndEncodesSpecialCharacters() {
        String input = "<script>alert('xss')</script>Hello & <b>World</b>";
        String result = InputSanitizer.sanitize(input);

        assertEquals("Hello &amp; &lt;b&gt;World&lt;/b&gt;", result);
    }

    @Test
    void sanitize_removesJavascriptProtocolAndEventHandlers() {
        String input = "<img src=x onerror=alert(1)>javascript:alert(1)";
        String result = InputSanitizer.sanitize(input);

        assertEquals("&lt;img src=x alert(1)&gt;alert(1)", result);
    }

    @Test
    void sanitize_preservesNormalText() {
        assertEquals("Onde Travel", InputSanitizer.sanitize("Onde Travel"));
    }
}
