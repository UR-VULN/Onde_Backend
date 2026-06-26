package com.onde.api.application.auth.support;

import com.onde.core.entity.member.MemberRole;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;

import java.util.EnumSet;
import java.util.Set;

/** 회원가입 API에서 허용되는 역할 화이트리스트 (USER, SELLER만) */
public final class SignupRolePolicy {

    private static final Set<MemberRole> ALLOWED_SIGNUP_ROLES = EnumSet.of(
            MemberRole.USER,
            MemberRole.SELLER
    );

    private SignupRolePolicy() {
    }

    public static MemberRole resolve(MemberRole requestedRole) {
        if (requestedRole == null) {
            return MemberRole.USER;
        }
        if (!ALLOWED_SIGNUP_ROLES.contains(requestedRole)) {
            throw new ForbiddenException(ErrorCode.SIGNUP_ROLE_NOT_ALLOWED);
        }
        return requestedRole;
    }
}
