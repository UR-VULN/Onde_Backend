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
}
