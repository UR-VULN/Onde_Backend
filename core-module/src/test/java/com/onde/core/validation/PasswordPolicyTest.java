package com.onde.core.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    @Test
    @DisplayName("3종 조합 8자 이상 사용자 비밀번호 허용")
    void acceptsUserPasswordWithThreeCategories() {
        assertTrue(PasswordPolicy.validate("Abcd1234!", PasswordPolicyLevel.USER).isEmpty());
    }

    @Test
    @DisplayName("2종 조합 10자 이상 사용자 비밀번호 허용")
    void acceptsUserPasswordWithTwoCategoriesAtTenChars() {
        assertTrue(PasswordPolicy.validate("abcdefgh12", PasswordPolicyLevel.USER).isEmpty());
    }

    @Test
    @DisplayName("약한 비밀번호 거부")
    void rejectsWeakPassword() {
        assertFalse(PasswordPolicy.validate("abcde123", PasswordPolicyLevel.USER).isEmpty());
        assertFalse(PasswordPolicy.validate("1", PasswordPolicyLevel.USER).isEmpty());
        assertFalse(PasswordPolicy.validate("aaaa", PasswordPolicyLevel.USER).isEmpty());
    }

    @Test
    @DisplayName("관리자 비밀번호는 4종 10자 이상")
    void enforcesAdminPolicy() {
        assertTrue(PasswordPolicy.validate("Admin1234!", PasswordPolicyLevel.ADMIN).isEmpty());
        assertFalse(PasswordPolicy.validate("Admin1234", PasswordPolicyLevel.ADMIN).isEmpty());
        assertFalse(PasswordPolicy.validate("admin1234!", PasswordPolicyLevel.ADMIN).isEmpty());
    }
}
