package com.onde.api.application.auth.support;

import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.UnauthorizedException;

/** 클라이언트가 전송한 memberId와 JWT 인증 주체 불일치 차단 */
public final class MemberIdBindingValidator {

    private MemberIdBindingValidator() {
    }

    public static void assertAuthenticatedMember(Long authenticatedMemberId) {
        if (authenticatedMemberId == null) {
            throw new UnauthorizedException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    public static void rejectForgedMemberId(Long authenticatedMemberId, Long requestMemberId) {
        assertAuthenticatedMember(authenticatedMemberId);
        if (requestMemberId != null && !requestMemberId.equals(authenticatedMemberId)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }
    }
}
