package com.onde.api.application.accommodation.dto;

import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ValidationException;

public enum ReportTemplateType {
    VERIFICATION("verification"),
    BUSINESS("business");

    private final String jsonValue;

    ReportTemplateType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    public String getJsonValue() {
        return jsonValue;
    }

    public boolean isBusiness() {
        return this == BUSINESS;
    }

    public static ReportTemplateType from(String value) {
        if (value == null || value.isBlank()) {
            return VERIFICATION;
        }
        String normalized = value.trim();
        for (ReportTemplateType type : values()) {
            if (type.jsonValue.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
    }
}
