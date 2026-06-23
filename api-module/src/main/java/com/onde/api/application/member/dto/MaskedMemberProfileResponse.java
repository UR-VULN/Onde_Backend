package com.onde.api.application.member.dto;

import com.onde.core.util.MaskingUtil;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MaskedMemberProfileResponse {
    private String email;
    private String name;
    private String phoneNumber;
    private String nickname;

    public static MaskedMemberProfileResponse from(MemberProfileResponse original) {
        return MaskedMemberProfileResponse.builder()
                .email(MaskingUtil.maskEmail(original.getEmail()))
                .name(original.getName())
                .phoneNumber(MaskingUtil.maskPhoneNumber(original.getPhoneNumber()))
                .nickname(original.getNickname())
                .build();
    }
}
