package com.onde.core.validation;

/**
 * API 입력값 크기·범위 상한 (1-6 입력 무결성 검증 공통 상수).
 */
public final class ValidationLimits {

    private ValidationLimits() {
    }

    // --- 문자열 길이 ---
    public static final int EMAIL_MAX = 320;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 128;
    public static final int NICKNAME_MIN = 2;
    public static final int NICKNAME_MAX = 30;
    public static final int NAME_MAX = 100;
    public static final int PHONE_MAX = 20;
    public static final int TITLE_MAX = 200;
    public static final int CONTENT_MAX = 10_000;
    public static final int COMMENT_MAX = 2_000;
    public static final int URL_MAX = 2_048;
    public static final int LICENSE_PLATE_MAX = 20;
    public static final int MODEL_NAME_MAX = 100;
    public static final int GENERIC_TEXT_MAX = 500;
    public static final int BOOKING_CODE_MAX = 50;
    public static final int PG_TX_ID_MAX = 100;
    public static final int PASSPORT_MAX = 20;
    public static final int ADDRESS_MAX = 500;
    public static final int DESCRIPTION_MAX = 5_000;
    public static final int BANK_ACCOUNT_MAX = 30;
    public static final int BUSINESS_NUMBER_MAX = 12;
    public static final int TOKEN_MAX = 512;

    // --- 숫자·페이지 ---
    public static final int PAGE_MIN = 0;
    public static final int PAGE_SIZE_MIN = 1;
    public static final int PAGE_SIZE_MAX = 100;
    public static final int RATING_MIN = 1;
    public static final int RATING_MAX = 5;
    public static final int GUESTS_MIN = 1;
    public static final int GUESTS_MAX = 20;
    public static final int AGE_MIN = 1;
    public static final int AGE_MAX = 150;
    public static final int MILEAGE_MAX = 1_000_000;
    public static final int PASSENGER_MAX = 9;
    public static final int LIST_MAX_SIZE = 100;

    // --- 금액 (원) ---
    public static final long AMOUNT_MIN = 0L;
    public static final long AMOUNT_MAX = 999_999_999L;
    public static final long WALLET_CHARGE_MAX = 1_000_000L;

    // --- 파일 ---
    public static final int MAX_IMAGES = 3;
    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
}
