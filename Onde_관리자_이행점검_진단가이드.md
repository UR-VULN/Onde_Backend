# Onde 관리자(Admin) 관점 취약점 이행진단 가이드 (조치 완료 항목 대상)

본 문서는 실제 취약점이 발견되어 이번에 코드 수정(조치)이 완료된 **핵심 취약점 항목(P0 ~ P2)**에 대해 버프 스위트(Burp Suite)를 이용하여 로컬에서 수행하는 **이행점검(재현 및 검증) 절차**만을 다룹니다.

---

## 1. 사전 진단 준비

### 로컬 포트 및 서비스 연결 상태
- **사용자 API (api-module)**: [http://localhost:8080](http://localhost:8080)
- **관리자 API (admin-module)**: [http://localhost:8081](http://localhost:8081)
- **프론트엔드 (React/Vite)**: [http://localhost:5173](http://localhost:5173) (Vite가 `127.0.0.1`에 정상 바인딩됨)
- **DB 터널링 (AWS RDS)**: 로컬 `3307` ➔ RDS `3306` 연결 상태
- **Redis 터널링 (AWS Redis)**: 로컬 `6379` ➔ Redis `6379` 연결 상태

### 버프 스위트 프록시 설정
1. Burp Suite의 **Proxy ➔ Proxy Settings ➔ Proxy Listeners**의 포트가 브라우저 프록시 연동 포트와 일치하는지 확인합니다. (기본 8080 포트는 사용자 API가 점유하고 있으므로, Burp의 프록시 포트를 **`8085`** 또는 **`8088`** 등으로 설정하여 연동하는 것을 권장합니다.)
2. 가장 권장하는 방법은 **Burp Suite 내장 브라우저(Proxy ➔ Intercept ➔ Open Browser)**를 사용하여 [http://127.0.0.1:5173](http://127.0.0.1:5173)에 접속하는 것입니다. 별도의 프록시 설정 없이도 즉시 통신 캡처가 가능합니다.

---

## 2. 취약점 조치 완료 항목별 이행점검 시나리오

### 🚀 P0 — [4-5] 역할전환 API Mass Assignment (SUPER_ADMIN 권한 획득 시도)
- **목적**: 회원 역할 변경 API 호출 시 인자 조작을 통해 비정상적인 등급(`SUPER_ADMIN`)으로 강제 승급하는지 검증합니다.
- **재현 대상 API**: `PATCH http://localhost:8081/api/v1/admin/members/{targetMemberId}/role`
- **검증 절차**:
  1. Burp Suite로 일반 회원 역할 수정 패킷을 캡처한 뒤, **Repeater**로 보냅니다.
  2. Request Body의 JSON 파라미터 값에 고위 관리자 등급을 강제 삽입하여 전송합니다.
     * **페이로드**:
       ```json
       {
         "roles": ["SUPER_ADMIN"]
       }
       ```
       또는
       ```json
       {
         "newRole": "SUPER_ADMIN"
       }
       ```
  3. **조치 결과 확인 (성공)**:
     - 서버가 **`500 Internal Server Error`** 혹은 **`400 Bad Request`**를 반환하며 응답 본문에 `"IllegalArgumentException: 허용되지 않은 권한 변경 요청입니다."` 예외 메시지가 명확히 노출되는지 확인합니다. (실제 DB의 role이 변경되지 않고 차단되어야 합니다.)



---

### 🚀 P0 — [4-5 / 1-3 연계] 일반계정 권한상승 ➔ 관리자 API 전체회원 탈취 (연계 검증)
- **목적**: 회원가입 시 role 필드 변조를 통한 SUPER_ADMIN 강제 생성 여부를 확인하고, 해당 계정의 토큰으로 전체 회원 리스트를 탈취해 내는 전체 침투 체인이 차단되었는지 다층 방어 구조를 검증합니다.
- **재현 대상 API**:
  1. 1단계: `POST http://localhost:8080/api/v1/auth/signup` (사용자 회원가입)
  2. 2단계: `GET http://localhost:8081/api/v1/admin/members` (관리자 전체 회원조회)
- **검증 절차**:
  1. 사용자 회원가입 패킷을 캡처하여 Repeater로 전송합니다.
  2. 가입 데이터 파라미터에 `role: "SUPER_ADMIN"` 필드를 수동으로 주입하여 가입 요청을 전송합니다.
     * **페이로드**:
       ```json
       {
         "email": "attacker@example.com",
         "password": "Password123!",
         "name": "공격자",
         "role": "SUPER_ADMIN"
       }
       ```
  3. 회원가입이 정상 완료되면, 해당 계정으로 로그인하여 발급받은 JWT access token을 복사합니다.
  4. 복사한 토큰을 사용하여 `GET /api/v1/admin/members` API를 요청합니다.
  5. **조치 결과 확인 (성공)**:
     - **회원가입 단계에서 `role: "SUPER_ADMIN"` 설정이 무시된 채 일반 `USER`로만 가입**되어야 하며, 해당 토큰으로 관리자 회원목록 API 호출 시 **`403 Forbidden`**으로 접근이 원천 차단되어 전체 회원 목록이 노출되지 않아야 합니다.

---

### ⚠️ P1 — [4-3 / 5-1] 관리자 Health Check 접근제어 우회 및 정보 노출
- **목적**: 무인증 상태로 헬스체크 API에 무단 접근할 수 있는지 확인하고, 접근에 성공하더라도 상세 내부 인프라 정보가 새어나가지 않는지 검증합니다.
- **재현 대상 API**: `GET http://localhost:8081/api/v1/admin/health`
- **검증 절차**:
  1. Burp Suite로 `GET /api/v1/admin/health` 요청을 생성하거나 가로챕니다.
  2. `Authorization` 헤더나 쿠키 등 인증 정보를 담지 않은 상태로 원격 서버에 요청을 전송합니다.
  3. **조치 결과 확인 (성공)**:
     - **`401 Unauthorized`** 혹은 **`403 Forbidden`** 에러 코드가 떨어지며 접근이 거부되어야 합니다.
     - (만약 로컬 개발망 등에서 권한이 통과되어 `200 OK`를 얻더라도) 응답 JSON 내용물에 디스크 사양, 운영체제 상세 경로, DB 버전(MariaDB 10.11), Redis 사양 등 민감한 정보가 보이지 않고 **`{ "status": "UP" }`** 형태의 단순 가동여부만 출력되어야 합니다.

---

### 🔍 P2 — [3-4] 관리자 도메인명 추측 가능 (CORS Origin 우회 점검)
- **목적**: 어드민 도메인이 변경된 상태에서 기존의 `admin.onde.click` 오리진(Origin)으로 요청을 위조하여 전송할 때 백엔드가 응답 헤더로 CORS를 허용하는지 검증합니다.
- **재현 대상 API**: `GET http://localhost:8080/api/v1/health` (또는 일반 API)
- **검증 절차**:
  1. API 요청 패킷을 캡처하여 Repeater로 전송합니다.
  2. Request Header의 `Origin` 값을 임의로 `https://admin.onde.click`으로 조작하여 전송합니다.
     * **헤더 수정**:
       `Origin: https://admin.onde.click`
  3. **조치 결과 확인 (성공)**:
     - 응답 헤더(Response Headers)에 **`Access-Control-Allow-Origin: https://admin.onde.click` 헤더가 노출되지 않아야** 성공입니다. (등록된 `https://rookies.onde.click` 등의 올바른 오리진 요청에만 CORS 헤더가 반환되어야 합니다.)
