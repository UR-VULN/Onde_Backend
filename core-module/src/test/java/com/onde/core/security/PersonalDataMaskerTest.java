package com.onde.core.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDataMaskerTest {

    @Test
    void maskEmail_hidesLocalAndDomainParts() {
        assertEquals("us***@ex***.com", PersonalDataMasker.maskEmail("user@example.com"));
    }

    @Test
    void maskName_hidesMiddleCharacters() {
        assertEquals("홍*동", PersonalDataMasker.maskName("홍길동"));
        assertEquals("김*", PersonalDataMasker.maskName("김철"));
    }

    @Test
    void maskPhone_hidesMiddleDigits() {
        assertEquals("010-****-5678", PersonalDataMasker.maskPhone("010-1234-5678"));
    }

    @Test
    void maskAccountNumber_masksMiddleSegment() {
        assertEquals("123-***-3456", PersonalDataMasker.maskAccountNumber("1234563456"));
        assertEquals("123-***-7890", PersonalDataMasker.maskAccountNumber("123-456-7890"));
    }
}
