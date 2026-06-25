package com.onde.admin.application.member.dto;

import com.onde.core.entity.member.MemberRole;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoleUpdateRequest {
    private List<MemberRole> roles;
    private MemberRole newRole;

    public MemberRole resolvePrimaryRole() {
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0);
        }
        return newRole;
    }

    public void validateSafeRole() {
        MemberRole targetRole = resolvePrimaryRole();
        if (targetRole == MemberRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("보안 경고: API를 통해 최고 관리자(SUPER_ADMIN) 권한을 부여할 수 없습니다.");
        }
    }
}
