package com.onde.core.entity.member;

public enum MemberStatus {
    ACTIVE,    // 활성 상태 (정상 이용 가능)
    PENDING,   // 승인 대기 (판매자 가입 승인 전)
    DORMANT,   // 휴면 상태 (장기 미접속)
    WITHDRAWN, // 탈퇴 상태 (서비스 이용 불가)
    BANNED  // 영구 정지 (관리자에 의한 블랙리스트)
}
