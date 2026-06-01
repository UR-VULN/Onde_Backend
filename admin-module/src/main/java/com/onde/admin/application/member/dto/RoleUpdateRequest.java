package com.onde.admin.application.member.dto;

import com.onde.core.entity.member.MemberRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateRequest {
    private MemberRole newRole;
}