package com.onde.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// 1. admin과 core 패키지의 컴포넌트(Service, Controller 등)를 모두 읽도록 설정
@SpringBootApplication(scanBasePackages = {"com.onde.admin", "com.onde.core"})
// 2. core 모듈에 있는 엔터티(DB 테이블 객체)들을 읽도록 설정
@EntityScan(basePackages = {"com.onde.core.entity"})
// 3. core 모듈에 있는 레포지토리(DB 접근 객체)들을 읽도록 설정
@EnableJpaRepositories(basePackages = {"com.onde.core.repository"})
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}