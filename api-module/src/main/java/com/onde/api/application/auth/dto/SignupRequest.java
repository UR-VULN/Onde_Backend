package com.onde.api.application.auth.dto;

import com.onde.core.entity.member.MemberRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {
    
    private MemberRole role; 
    private String email;
    private String password;
    private String phoneNumber;
}