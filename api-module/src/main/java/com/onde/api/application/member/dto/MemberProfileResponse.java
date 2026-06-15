package com.onde.api.application.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberProfileResponse {
    private String email;
    private String name;
    private String phoneNumber;
    private String nickname;
}
