package com.onde.api.application.auth.support;

import com.onde.core.entity.member.MemberRole;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignupRolePolicyTest {

    @Test
    void defaultsNullToUser() {
        assertEquals(MemberRole.USER, SignupRolePolicy.resolve(null));
    }

    @Test
    void allowsUserAndSeller() {
        assertEquals(MemberRole.USER, SignupRolePolicy.resolve(MemberRole.USER));
        assertEquals(MemberRole.SELLER, SignupRolePolicy.resolve(MemberRole.SELLER));
    }

    @ParameterizedTest
    @EnumSource(value = MemberRole.class, names = {"USER", "SELLER"}, mode = EnumSource.Mode.EXCLUDE)
    void rejectsPrivilegedRoles(MemberRole role) {
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> SignupRolePolicy.resolve(role));
        assertEquals(ErrorCode.SIGNUP_ROLE_NOT_ALLOWED, ex.getErrorCode());
    }
}
