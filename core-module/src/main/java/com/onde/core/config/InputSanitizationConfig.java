package com.onde.core.config;

import com.onde.core.security.InputSanitizationEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InputSanitizationConfig {

    @Bean
    public InputSanitizationEventListener inputSanitizationEventListener() {
        return new InputSanitizationEventListener();
    }
}
