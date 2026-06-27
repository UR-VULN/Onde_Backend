package com.onde.admin.application.member.dto;

import com.onde.core.entity.member.MemberRole;
import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoleUpdateRequest {

    @Size(max = ValidationLimits.LIST_MAX_SIZE, message = "roles 목록은 100개 이하여야 합니다.")
    private List<MemberRole> roles;

    private MemberRole newRole;

    public MemberRole resolvePrimaryRole() {
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0);
        }
        return newRole;
    }
}
