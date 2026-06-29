package com.onde.api.application.accommodation.dto;

public record RoomInventoryUpdateResponse(
    Long roomId,
    String message // 예: "해당 날짜의 재고 및 가격이 성공적으로 수정되었습니다."
) {}