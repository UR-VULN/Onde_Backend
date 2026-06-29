package com.onde.core.support;

import lombok.Getter;

@Getter
public class ErrorDetail {
    private final String field;
    private final Object rejectedValue;
    private final String reason;

    public ErrorDetail(String field, Object rejectedValue, String reason) {
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.reason = reason;
    }
}
