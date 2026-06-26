package com.onde.api.application.accommodation.dto;

import com.onde.core.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTemplateTypeTest {

    @Test
    void defaultsToVerificationWhenBlank() {
        assertTrue(ReportTemplateType.from(null).isBusiness() == false);
        assertTrue(ReportTemplateType.from("  ").isBusiness() == false);
    }

    @Test
    void acceptsVerificationAndBusinessOnly() {
        assertFalse(ReportTemplateType.from("verification").isBusiness());
        assertTrue(ReportTemplateType.from("business").isBusiness());
    }

    @Test
    void rejectsUnknownTemplate() {
        assertThrows(ValidationException.class, () -> ReportTemplateType.from("../../../etc/passwd"));
    }
}
