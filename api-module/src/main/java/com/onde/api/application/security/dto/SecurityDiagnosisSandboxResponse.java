package com.onde.api.application.security.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecurityDiagnosisSandboxResponse {
    private String lfiResult;
    private String ssrfResult;
}
