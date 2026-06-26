package com.onde.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CorsOriginProperties.class, LoginLockProperties.class, AuthCookieProperties.class})
public class CoreSecurityConfiguration {
}
