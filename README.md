# ✈️ Onde Backend · 관리자 영역 이행 점검 브랜치

> 브랜치: `audit/admin-remediation/yr`
>
> 진단 기준 시점인 `audit/initial`(2026-06-17)에서 분기해, 수동 진단에서 나온 **관리자 영역(admin-module)** 취약점의 조치와 관련 공통 설정(api-module)의 조치를 반영한 이행 점검 브랜치입니다.
> 진단 기준은 SK쉴더스 Web/API 개발보안 가이드(2022)이며, 이행 점검은 팀원별 `audit/user-remediation/*`, `audit/admin-remediation/*` 브랜치로 나눠 진행했습니다.
> 사용자 영역 브랜치와는 따로 분기했기 때문에, 그쪽 변경은 이 브랜치에 들어 있지 않습니다.
>
> 서비스 전체 소개와 실행 방법은 [main 브랜치 README](https://github.com/UR-VULN/Onde_Backend/tree/main)를 참고하세요.

---

## 1. 변경 범위

`audit/initial` 대비 커밋 1개, 파일 20개가 바뀌었습니다.

* 비교: [audit/initial...audit/admin-remediation/yr](https://github.com/UR-VULN/Onde_Backend/compare/audit/initial...audit/admin-remediation/yr)
* 대상 모듈: `admin-module`(관리자 API), `api-module`(보안 설정 · 예외 처리), `core-module`(JWT)
* `admin-module`이 로그인 요청 DTO를 재사용하도록 `api-module`을 의존합니다 (`admin-module/build.gradle`).

---

## 2. 관리자 영역 (admin-module)

| 진단 항목 | 조치 | 주요 파일 |
|---|---|---|
| 3-2 인증 실패 횟수 제한 | 관리자 로그인 API `POST /api/v1/admin/auth/login` 추가. 실패 횟수를 Redis에 기록하고 5회 실패하면 30분 동안 잠금 (잠금 중 로그인은 403) | `AdminAuthController`, `AdminAuthService` |
| 3-3 계정 정보 파악 | 없는 계정과 틀린 비밀번호에 같은 메시지("아이디 또는 비밀번호가 일치하지 않습니다.")로 401 응답 | `AdminGlobalExceptionHandler` |
| 4-1 쿠키 조작 | 관리자 로그인 토큰을 HttpOnly · Secure · `SameSite=Lax` 쿠키로 전달 | `AdminAuthController` |
| 4-5 일반계정 권한 상승 | 역할 변경 API로 `SUPER_ADMIN`을 부여하지 못하게 차단 | `RoleUpdateRequest`, `AdminMemberService` |
| 1-3 파라미터 조작 | 마커 API가 관리자 식별값을 요청 헤더(`X-Admin-Id`) 대신 JWT 검증을 마친 `Authentication`에서 가져옴 | `AdminMarkerController` |
| 1-6 입력 값 크기 및 무결성 | 마커 위도(-90 ~ 90) · 경도(-180 ~ 180) 범위, 게시글 블라인드 사유 255자 제한 | `AdminMarkerRequest`, `AdminBlindRequest` |
| 6-1 · 6-2 오류 처리 | 중복된 예외 처리기(`GlobalAdminExceptionHandler`)를 `AdminGlobalExceptionHandler` 하나로 합치고 적용 범위를 `com.onde.admin`으로 지정. 400 · 401 · 403 · 404 · 405 · 415 · 500을 내부 메시지 없이 공통 형식으로 응답하고, 없는 경로도 JSON 404로 응답 | `AdminGlobalExceptionHandler`, `application.yml` |
| 7-4 취약한 보안 설정 | CORS 허용 출처를 `http://localhost:5173`, `https://onde.click`으로 제한하고 CSP(`default-src 'self'; script-src 'self'; object-src 'none'`) · X-XSS-Protection 헤더 추가 | `AdminSecurityConfig` |

---

## 3. 공통 설정 (api-module · core-module)

| 진단 항목 | 조치 | 주요 파일 |
|---|---|---|
| 1-1 CSRF | CSRF 보호를 켜고 토큰을 `XSRF-TOKEN` 쿠키(`CookieCsrfTokenRepository`)로 전달. `/api/v1/admin/login`, `/api/v1/health`는 제외 | `SecurityConfig` |
| 4-1 쿠키 조작 | 로그인 응답의 토큰 쿠키를 `SameSite=None` → `Lax`로 변경 | `AuthController` |
| 4-2 인증 값 안전성 | 유효하지 않은 토큰이면 `SecurityContext`를 비우고, 서명 · 형식 · 만료 오류를 구분해 기록 | `JwtAuthenticationFilter`, `JwtTokenProvider` |
| 4-3 접근제어 우회 | `StrictHttpFirewall`로 백슬래시 · 인코딩된 슬래시 · 세미콜론이 들어간 경로 차단. 관리자 경로 규칙을 가장 먼저 검사하고, 공개 경로는 `/api/v1/health`, `/api/v1/auth/**`, `/error`만 남김 | `SecurityConfig` |
| 6-1 오류 페이지 정보 노출 | 오류 응답에서 예외 클래스명 · 내부 메시지를 제거하고 적용 범위를 `com.onde.api`로 지정 | `GlobalExceptionHandler` |

> 참고: 이 브랜치의 api-module `SecurityConfig`에는 OAuth2 소셜 로그인 설정과 숙소 · 렌터카 · 항공 검색 등 비로그인 조회 경로가 포함되어 있지 않습니다.

---

## 4. 브랜치 구성

| 브랜치 | 내용 |
|---|---|
| `audit/initial` | 진단 기준 시점 |
| `audit/user-remediation/*` | 사용자 · 판매자 영역 이행 점검 (팀원별) |
| `audit/admin-remediation/*` | 관리자 영역 이행 점검 (팀원별) |
| `main` | 서비스 기준 브랜치 |
