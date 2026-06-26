package com.onde.api.application.auth.support;

import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberIdBindingValidatorTest {

    @Test
    void rejectsForgedMemberId() {
        assertThrows(ForbiddenException.class, () ->
                MemberIdBindingValidator.rejectForgedMemberId(1L, 2L));
    }

    @Test
    void allowsMatchingOrAbsentMemberId() {
        assertDoesNotThrow(() -> MemberIdBindingValidator.rejectForgedMemberId(1L, 1L));
        assertDoesNotThrow(() -> MemberIdBindingValidator.rejectForgedMemberId(1L, null));
    }

    @Test
    void rejectsMissingAuthenticatedMember() {
        assertThrows(UnauthorizedException.class, () ->
                MemberIdBindingValidator.rejectForgedMemberId(null, 1L));
    }
}
