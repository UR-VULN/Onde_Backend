package com.onde.api.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;

@JsonComponent
public class XssConfig {

    public static class HtmlEscapeStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null) {
                return null;
            }
            // <, >, ", ' 등을 &lt;, &gt; 등으로 안전하게 치환 (XSS 방어)
            return HtmlUtils.htmlEscape(value);
        }

        @Override
        public Class<String> handledType() {
            return String.class;
        }
    }
}