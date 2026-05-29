package com.onde.api.application.admin.dto;

import com.onde.core.entity.member.MemberRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateRequest {
    private MemberRole newRole;
}