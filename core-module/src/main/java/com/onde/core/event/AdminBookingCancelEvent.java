package com.onde.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 어드민 직권으로 예약이 강제 취소되었을 때 발행되는 이벤트입니다.
 * 결제/정산 도메인에서 이를 리스닝하여 마일리지 복구 및 대금 차감 등을 비동기 처리합니다.
 */
@Getter
public class AdminBookingCancelEvent extends ApplicationEvent {
    
    private final Long bookingId;
    private final Long memberId;
    private final String targetType; // FLIGHT, ROOM, CAR 등

    public AdminBookingCancelEvent(Object source, Long bookingId, Long memberId, String targetType) {
        super(source);
        this.bookingId = bookingId;
        this.memberId = memberId;
        this.targetType = targetType;
    }
}
