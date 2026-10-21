# ✈️ Onde Backend · 사용자 영역 이행 점검 브랜치

> 브랜치: `audit/user-remediation/yr`
>
> 진단 기준 시점인 `audit/initial`(2026-06-17)에서 분기해, 수동 진단에서 나온 **사용자 · 판매자 영역(api-module)** 취약점의 조치를 반영한 이행 점검 브랜치입니다.
> 진단 기준은 SK쉴더스 Web/API 개발보안 가이드(2022)이며, 이행 점검은 팀원별 `audit/user-remediation/*`, `audit/admin-remediation/*` 브랜치로 나눠 진행했습니다.
>
> 서비스 전체 소개와 실행 방법은 [main 브랜치 README](https://github.com/UR-VULN/Onde_Backend/tree/main)를 참고하세요.

---

## 1. 변경 범위

`audit/initial` 대비 커밋 1개, 파일 19개가 바뀌었습니다.

* 비교: [audit/initial...audit/user-remediation/yr](https://github.com/UR-VULN/Onde_Backend/compare/audit/initial...audit/user-remediation/yr)
* 대상 모듈: `api-module`(사용자 · 판매자 API), `core-module`(회원 · 토큰 엔티티, JWT)

---

## 2. 항목별 조치 내역

| 진단 항목 | 조치 | 주요 파일 |
|---|---|---|
| 1-1 XSS | 게시글 제목 · 내용을 HTML 이스케이프해 저장하고, JSON 요청 본문의 문자열도 역직렬화 단계에서 이스케이프 | `PostController`, `XssConfig` |
| 1-4 SSRF / File Inclusion | 통합 리포트 API(`POST /api/v1/report/integrated`)에서 진단용으로 넣어 둔 코드(템플릿 경로로 서버 파일 읽기, 로고 URL로 서버가 직접 요청)를 제거하고, 템플릿(`verification`, `business`)과 로고 URL을 화이트리스트로 제한. 벗어나면 403 | `IntegratedReportController` |
| 1-6 입력 값 크기 및 무결성 | 항공 예약 요청에 `@Valid` 적용: 스케줄 · 좌석 등급 필수, 이름 100자 · 여권 번호 20자 제한, 생년월일은 과거 날짜, 총 가격은 0 초과 | `FlightController`, `FlightBookingRequest` |
| 2-1 악성코드파일 업로드 | 게시글 이미지는 `jpg`, `jpeg`, `png`, `gif`, `webp`만 허용하고 파일명의 경로 문자(`..`, `/`, `\`)를 차단. 저장 파일명은 UUID로 새로 만듦 | `PostService`, `MockS3Uploader` |
| 3-2 인증 실패 횟수 제한 | 로그인 비밀번호 5회 실패 시 계정 잠금, 로그인 성공 시 실패 횟수 초기화 (일반 · 관리자 로그인 모두) | `Member`, `AuthService` |
| 3-3 계정 정보 파악 | 없는 이메일과 틀린 비밀번호에 같은 메시지("이메일 또는 비밀번호가 일치하지 않습니다.")로 응답 | `AuthService` |
| 4-1 쿠키 조작 | 로그인 응답의 토큰 쿠키를 `SameSite=None` → `Lax`로 변경 (HttpOnly · Secure 유지) | `AuthController` |
| 4-2 인증 값 안전성 | `alg=none`이거나 세 부분 구조가 아닌 JWT는 필터에서 401로 차단, 서명 · 형식 · 만료 오류를 구분해 기록. 로그인 응답 본문에서 토큰을 빼고 쿠키로만 전달. Refresh Token의 Redis 키를 이메일 대신 무작위 UUID로 바꾸고 이메일은 인덱스 필드로 보관 | `JwtAuthenticationFilter`, `JwtTokenProvider`, `LoginResponse`, `RefreshToken` |
| 4-3 접근제어 우회 | `StrictHttpFirewall`로 인코딩된 슬래시 · 이중 슬래시 · 마침표와 세미콜론이 들어간 경로 차단. 헬스체크(`/api/v1/health`)는 `127.0.0.1`에서만 허용, `/api/v1/admin/**`은 관리자 역할만 허용 | `SecurityConfig` |
| 6-1 오류 페이지 정보 노출 | 오류 응답에서 예외 클래스명 · 내부 메시지를 제거하고, 없는 경로는 404, 잘못된 JSON은 400, 보안 차단은 403으로 공통 형식 응답 | `GlobalExceptionHandler` |

---

## 3. 클라이언트에서 달라지는 점

* 로그인 실패는 이메일이 없든 비밀번호가 틀리든 400과 같은 메시지로 응답합니다. 5회 연속 실패하면 잠금 안내 메시지가 나옵니다.
* 로그인 응답 본문에는 토큰이 없고, `accessToken` · `refreshToken` 쿠키(HttpOnly · Secure · SameSite=Lax)로만 전달됩니다.
* 게시글 이미지는 위 5개 확장자만 올릴 수 있습니다.
* 통합 리포트 API는 허용된 템플릿과 로고 URL만 받습니다.

---

## 4. 브랜치 구성

| 브랜치 | 내용 |
|---|---|
| `audit/initial` | 진단 기준 시점 |
| `audit/user-remediation/*` | 사용자 · 판매자 영역 이행 점검 (팀원별) |
| `audit/admin-remediation/*` | 관리자 영역 이행 점검 (팀원별) |
| `main` | 서비스 기준 브랜치 |
