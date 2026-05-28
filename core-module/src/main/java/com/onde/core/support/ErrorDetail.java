package com.onde.core.support;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ErrorDetail {
    private String field;
    private String rejectedValue;
    private String reason;

    public ErrorDetail(String field, String rejectedValue, String reason) {
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.reason = reason;
    }
}


