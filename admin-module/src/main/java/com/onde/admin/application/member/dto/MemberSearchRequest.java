package com.onde.admin.application.member.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class MemberSearchRequest {
    private String role; // USER, SELLER 등 (null이면 전체)
    private String status; // ACTIVE, DORMANT 등 (null이면 전체)
    private String name; // 이름 또는 이메일 검색어
    private String keyword; // 기존 호출 호환용 검색어
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate; // 가입일 검색 시작
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;   // 가입일 검색 종료
}
