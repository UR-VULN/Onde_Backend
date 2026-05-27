package com.onde.core.support;

import lombok.Builder;
import lombok.Getter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 서비스 전체에서 공통으로 사용할 API 응답 규격을 정의하는 제네릭 클래스입니다.
 * 성공 여부, 응답 데이터, 메시지, 그리고 UTC 기준 타임스탬프를 일관성 있게 반환합니다.
 *
 * @param <T> 응답 본문(data)의 타입
 */
@Getter
@Builder
public class ApiResponse<T> {

    /**
     * API 호출 성공 여부 (true: 성공, false: 실패)
     */
    private boolean success;

    /**
     * API 응답 본문 데이터
     */
    private T data;

    /**
     * API 결과에 대한 설명 또는 오류 관련 안내 메시지
     */
    private String message;

    /**
     * API 응답이 생성된 UTC 기준 시간 (ISO 8601 형식: yyyy-MM-dd'T'HH:mm:ss.SSS'Z')
     */
    private String timestamp;

    /**
     * 특정 데이터와 커스텀 메시지를 포함하여 성공 응답을 생성합니다.
     *
     * @param data    응답할 데이터 본문
     * @param message 성공 안내 메시지
     * @param <T>     데이터의 제네릭 타입
     * @return ApiResponse 성공 객체
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT))
                .build();
    }

    /**
     * 특정 데이터를 포함하여 기본 성공 메시지("작업이 성공적으로 완료되었습니다.")와 함께 성공 응답을 생성합니다.
     *
     * @param data 응답할 데이터 본문
     * @param <T>  데이터의 제네릭 타입
     * @return ApiResponse 성공 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "작업이 성공적으로 완료되었습니다.");
    }
}

