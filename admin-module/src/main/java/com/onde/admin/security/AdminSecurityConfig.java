package com.onde.admin.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class AdminSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new AdminJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/admin/notifications/broadcast").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/v1/admin/posts/**", "/api/v1/admin/markers/**").hasAnyRole("SUPER_ADMIN", "GENERAL_ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "GENERAL_ADMIN", "SALES_ADMIN")
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
