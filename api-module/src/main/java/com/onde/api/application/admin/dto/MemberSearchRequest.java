package com.onde.api.application.admin.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class MemberSearchRequest {
    private String role; // USER, SELLER 등 (null이면 전체)
    private String keyword; // 이름 또는 이메일 검색어
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate; // 가입일 검색 시작
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;   // 가입일 검색 종료
}