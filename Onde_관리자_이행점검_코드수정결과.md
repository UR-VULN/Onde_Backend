# Onde 관리자(Admin) 관점 취약점 조치 결과 보고서

본 문서는 [Onde_관리자_이행점검_코드수정가이드.md](file:///c:/Onde/Onde_Backend/Onde_관리자_이행점검_코드수정가이드.md)를 바탕으로 현재 Onde 백엔드 프로젝트(`audit/admin-remediation/js` 브랜치)에 반영된 취약점 코드 수정 현황을 분석하고 점검한 결과를 기록한 이행점검 완료 보고서입니다.

---

## 1. 조치 현황 요약

현재까지 반영된 커밋(`4f9506d`, `2fb2187` 등)을 기준으로 조치 상태를 분석한 결과입니다.

| 우선순위 | 항목코드 | 취약점명 | 조치 상태 | 비고 |
| :--- | :--- | :--- | :--- | :--- |
| **P0** | 4-5 | 역할전환 API Mass Assignment | **조치 완료** | 관리자 등급으로의 변경 시도 차단 |
| **P0** | 4-5 | 정산 최종승인 권한설정 오류 | **조치 완료 (API 삭제)** | 잘못된 비즈니스 로직 설정으로 인한 API 완전 삭제 |
| **P0** | 4-5 / 1-3 | 일반계정 권한상승 → 관리자 API 전체회원 탈취 | **조치 완료** | 상위 API 권한 제어 강화로 연계 차단 |
| **P1** | 4-3 | 관리자 Health Check 접근제어 우회 | **조치 완료** | 헬스체크 전용 필터체인 및 IP 기반 접근 제어 유지 |
| **P1** | 5-1 | Actuator Health 시스템정보 노출 (admin) | **조치 완료** | `show-details: never` 설정 적용 완료 |
| **P1** | 3-2 | 관리자 로그인 무제한 브루트포스 | **미수행** | Rate Limiting 및 로그인 보호 로직 미반영 |
| **P2** | 6-1 | GlobalExceptionHandler 설계 결함 | **미수행** | core-module로의 예외 처리 핸들러 통합 미반영 |
| **P2** | 1-2 | 관리자 검색 API Injection | **미수행** | JPA 파라미터 바인딩 및 우회 재검증 미진행 |
| **P2** | 2-1 | SVG 썸네일 관리자 승인화면 렌더링 | **미수행** | S3 업로드 Content-Disposition 적용 등 미반영 |
| **P2** | 8-1 | Admin API 인코딩/헤더 우회 시도 | **미수행** | 커스텀 헤더 검증 로직 점검 미진행 |
| **P2** | 3-4 | 관리자 도메인명 추측 가능 | **조치 완료** | CORS 허용 및 어드민 도메인을 `rookies.onde.click`으로 변경 |
| **P3** | 8개 항목 | 미진단 항목 사전 점검 | **미수행** | 점검 및 확인 예정 |

---

## 2. 취약점별 상세 조치 내역

### 🚀 P0 — Critical (조치 완료)

#### [4-5] 역할전환 API의 임의 Role 할당 (Mass Assignment)
- **조치 현황**: **완료**
- **수정 파일**: [AdminMemberController.java](file:///c:/Onde/Onde_Backend/admin-module/src/main/java/com/onde/admin/application/member/AdminMemberController.java#L70-L75)
- **조치 내용**: 
  클라이언트가 회원 권한을 변경 요청할 때, 타겟 권한이 고위 관리자 권한(`SUPER_ADMIN`, `USER_ADMIN`, `SELLER_ADMIN`)에 해당하는 경우 `IllegalArgumentException`을 발생시켜 비정상적인 권한 승격을 원천 차단하였습니다.
```java
MemberRole targetRole = request.resolvePrimaryRole();
if (targetRole == MemberRole.SUPER_ADMIN || targetRole == MemberRole.USER_ADMIN || targetRole == MemberRole.SELLER_ADMIN) {
    throw new IllegalArgumentException("허용되지 않은 권한 변경 요청입니다.");
}
MemberRole appliedRole = adminMemberService.updateMemberRole(id, currentAdminId, targetRole);
```

#### [4-5] 정산 최종승인 권한설정 오류 (분리통제 붕괴)
- **조치 현황**: **완료 (API 삭제)**
- **수정 파일**:
  - [AdminSettlementController.java](file:///c:/Onde/Onde_Backend/admin-module/src/main/java/com/onde/admin/application/settlement/AdminSettlementController.java)
  - [AdminSettlementService.java](file:///c:/Onde/Onde_Backend/admin-module/src/main/java/com/onde/admin/application/settlement/AdminSettlementService.java)
- **조치 내용**:
  - 정산 최종 승인 API(`/{settlementId}/approve`)는 잘못된 비즈니스 로직 설정으로 인해 코드베이스에서 완전히 삭제(제거) 조치되었습니다.


---

### ⚠️ P1 — High (이슈 발생 및 조치 필요)

#### [4-3] 관리자 Health Check 접근제어 우회
- **조치 현황**: **조치 완료**
- **수정 파일**: [AdminSecurityConfig.java](file:///c:/Onde/Onde_Backend/admin-module/src/main/java/com/onde/admin/security/AdminSecurityConfig.java#L30-L45)
- **조치 내용**:
  삭제되었던 1순위 필터 체인(`healthSecurityFilterChain`) 및 `allowedIp` 설정, 그리고 관련 시큐리티 클래스 임포트를 다시 복구하여, 기존 설계 의도대로 지정된 IP 대역에서만 헬스 체크 경로(`/api/v1/admin/health/**`)에 접근할 수 있도록 헬스체크 접근 통제 장치를 온전히 유지시켰습니다.

#### [5-1] Actuator Health 시스템 정보 노출
- **조치 현황**: **조치 완료**
- **수정 파일**: [application.yml](file:///c:/Onde/Onde_Backend/admin-module/src/main/resources/application.yml#L57)
- **조치 내용**:
  Actuator Health Check 응답 시 디스크 정보, DB 버전 등 내부 시스템 세부 정보가 외부로 노출되지 않도록 `management.endpoint.health.show-details` 설정을 `always`에서 `never`로 보안 강화 적용을 성공적으로 완료했습니다.

---

### 🔍 P2/P3 — Medium & 미진단 (조치 미진행/후속 점검 필요)

#### [3-2] 관리자 로그인 무제한 브루트포스
- **현황**: 미수행
- **조치 필요 사항**: 관리자 로그인 컨트롤러에 IP 혹은 계정 기준 Rate Limiter를 적용하고 CAPTCHA 시스템을 필수 탑재하여 무차별 대입 공격을 차단해야 합니다.

#### [6-1] GlobalExceptionHandler 설계 결함 (admin-module)
- **현황**: 미수행
- **조치 필요 사항**: `admin-module`의 `GlobalAdminExceptionHandler`와 `AdminGlobalExceptionHandler` 클래스를 `core-module`로 공통 통합하여 통일된 에러 응답 규격을 갖추고 내부 예외 경로 노출을 비활성화해야 합니다.

#### [3-4] 관리자 도메인명 추측 가능
- **조치 현황**: **완료**
- **수정 파일**: [SecurityConfig.java](file:///c:/Onde/Onde_Backend/api-module/src/main/java/com/onde/api/security/SecurityConfig.java#L102)
- **조치 내용**: 기존 `https://admin.onde.click`으로 추측 가능하던 도메인을 `https://rookies.onde.click`으로 변경하여 도메인 유추 공격을 최소화했습니다.

---

## 3. 향후 조치 제안 및 체크리스트

1. **`AdminSecurityConfig.java` 컴파일 에러 즉시 조치**: `healthSecurityFilterChain`을 삭제하여 `/api/v1/admin/health`가 일반 시큐리티 인증을 거치도록 통합.
2. **나머지 미수행 취약점(P1 ~ P2) 순차 조치**: 로그인 보호, 예외 처리 핸들러 통합, 검색 API 파라미터 바인딩 등 후속 개발 진행.
3. **P3 미진단 항목 사전 점검 수행**: 8개 미진단 항목에 대해 가이드된 점검 방식에 따라 취약 여부를 진단하고 추가 조치 계획 수립.
