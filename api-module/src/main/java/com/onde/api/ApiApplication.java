package com.onde.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * API 모듈의 시작점인 메인 애플리케이션 클래스입니다.
 * 멀티모듈 구조로 설계되어 있어, core-module의 빈(Bean), 엔티티(Entity), 리포지토리(Repository) 등을 스캔하도록 설정되어 있습니다.
 */
@EnableScheduling // 정산 스케줄러(SettlementScheduler) 동작을 위해 스케줄링 기능을 활성화합니다.
@SpringBootApplication(scanBasePackages = { "com.onde.api", "com.onde.core" }) // api-module과 core-module의 패키지를 스캔 대상에 포함합니다.
@EntityScan(basePackages = "com.onde.core.entity") // core-module 내의 JPA 엔티티들을 감지하고 스캔하도록 설정합니다.
@EnableJpaRepositories(basePackages = "com.onde.core.repository") // core-module 내의 Spring Data JPA 리포지토리들을 스캔 및 등록합니다.
public class ApiApplication {
    public static void main(String[] args) {
        System.out.println("Starting ApiApplication...");
        SpringApplication.run(ApiApplication.class, args);
    }
}

