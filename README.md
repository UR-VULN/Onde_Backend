# ✈️ Onde (온데) Backend - 통합 여행 및 예약 플랫폼

> **Onde**는 항공권, 숙박, 렌터카 등 여행에 필요한 모든 서비스를 한곳에서 편리하게 예약하고 관리할 수 있는 차세대 통합 여행 플랫폼입니다.  
> 확장성과 독립성을 위해 **Spring Boot 멀티 모듈 아키텍처**를 채택하였으며, 대규모 트래픽 처리를 위한 분산 캐싱 및 정산 스케줄링을 지원합니다.

---

## 1. 프로젝트 소개
* **프로젝트 명**: Onde (온데)
* **개발 목적**: 항공, 숙박, 렌터카 서비스를 유기적으로 연결하여 끊김 없는 사용자 여행 경험을 제공하고, 관리자에게는 매출 관리 및 파트너 어드민 도구를 지원합니다.
* **핵심 지향점**:
  * **멀티 모듈 구조**: 공통 도메인 영역과 사용자.판매자/관리자 서비스를 완전히 분리하여 각 모듈의 독립적인 진화 가능.
  * **안정적인 예약 관리**: 동시성 제어 및 실시간 재고 관리 시스템 구축.
  * **인프라 자동화**: Docker Compose 환경 구축을 통한 빠른 개발 환경 구성 및 데이터 초기 시딩 자동화.

---

## 2. 주요 기능
### 🚗 실시간 렌터카 검색 및 예약
* **위치 기반 차량 검색**: 프론트엔드에 정의된 25개 주요 도시별 차량 목록 필터링.
* **재고 및 요금 실시간 조회**: 렌터카 재고(Inventory) 테이블과의 일자별 조인을 통한 실시간 예약 가능 여부 및 최저 가격 연동.
* **예약 흐름**: 차량 선택 -> 대여 및 반납 일자 입력 -> 결제 및 예약 상태 생성.

### 🏨 숙소 및 객실 예약
* **숙소 검색**: 카테고리, 지역별 필터링 기능 제공.
* **실시간 객실 재고 제어**: 동시 예약 발생 시 초과 예약(Overbooking)을 방지하기 위한 트랜잭션 관리.

### 🛫 실시간 항공권 검색 및 예약 (진행 중)
* **스케줄 및 노선 조회**: 판매자 승인 상태가 완료된 항공 노선 및 상세 스케줄 필터링.
* **실시간 타임아웃 스케줄러**: 결제 대기 시간 초과 시 자동으로 예약을 취소하는 스케줄러 구동.

### 👤 권한 기반 인증/인가 (Spring Security + JWT)
* **통합 회원 가입 및 로그인**: 일반 사용자 및 관리자 통합 JWT 발급.
* **어드민 허브**: 권한(`USER`, `SELLER`, `SUPER_ADMIN`, `USER_ADMIN`, `SELLER_ADMIN`)에 따른 접근 통제 및 백오피스 대시보드 API.

---

## 3. 기술 스택

### Cores
* **Java 17**
* **Spring Boot 3.2.5**
* **Gradle 8.7**

### Database & Storage
* **MariaDB 11.4** (메인 RDBMS)
* **Redis 7.2** (분산 락, 캐싱, 세션 관리)
* **MinIO** (로컬 S3 Mock 오브젝트 스토리지)
* **Flyway 9.22** (데이터베이스 스키마 마이그레이션 버전 관리)

### Dev & Ops
* **Docker / Docker Compose** (컨테이너 오케스트레이션)
* **Lombok / SLF4J (Logback)**

---

## 4. 빠른 시작 (Quick Start)

### 필수 요구사항
* 로컬 환경에 **Docker Desktop**이 설치되어 있어야 합니다.

### 실행 방법
1. **프로젝트 루트 디렉토리로 이동**
   ```bash
   cd c:/Users/user/Desktop/onde
   ```

2. **도커 컨테이너 전체 빌드 및 기동**
   ```bash
   # 데이터베이스, 레디스, 미니오, 백엔드(API/Admin), 프론트엔드가 한 번에 실행됩니다.
   docker compose up --build -d
   ```

3. **로컬 데이터 시드(Seed) 확인**
   * 서버 기동 완료 후, `db-seeder` 컨테이너가 동작하여 `/DB_Seed` 내에 있는 기초 데이터(회원, 숙소, 객실, 렌터카 270대 등)를 MariaDB에 자동으로 로딩합니다.

4. **로컬 접속 정보**
   * **사용자 API (api-module)**: `http://localhost:8080`
   * **관리자 API (admin-module)**: `http://localhost:8081`
   * **데이터베이스 (MariaDB)**: `localhost:3306`

---

## 5. 폴더 구조
본 프로젝트는 관심사 분리를 극대화하기 위해 멀티 모듈 프로젝트로 관리되고 있습니다.

```text
Onde_Backend/
├── core-module/          # 공통 도메인 엔티티, 리포지토리 및 공통 비즈니스 로직
│   └── src/main/resources/db/migration/  # Flyway 마이그레이션 SQL 파일 보관함
├── api-module/           # 일반 사용자용 REST API 및 인증(Security) 계층
│   └── src/main/java/com/onde/api/application/
│       ├── accommodation/ # 숙소 및 렌터카 컨트롤러/서비스
│       └── notification/  # 알림 및 푸시 기능 (Firebase Cloud Messaging)
├── admin-module/         # 백오피스 관리자용 전용 API 및 통계 비즈니스 계층
├── gradle/               # Gradle 래퍼 환경 설정 파일
├── build.gradle          # 루트 멀티 모듈 전체 빌드 스크립트
└── settings.gradle       # 하위 모듈(core, api, admin) 정의 설정 파일
```

---

## 6. 아키텍처 개요
```mermaid
graph TD
    subgraph Client["Client"]
        FE[Frontend - Vite/React]
    end

    subgraph ServiceLayer["Service Layer (Multi-Module)"]
        API[api-module: Port 8080]
        ADMIN[admin-module: Port 8081]
        CORE[core-module: Shared Entities/Repos]
    end

    subgraph DataLayer["Data Layer"]
        DB[(MariaDB)]
        RD[(Redis Cache)]
        S3[(MinIO Storage)]
    end

    FE -->|User Requests| API
    FE -->|Admin Requests| ADMIN
    API -.->|Depends on| CORE
    ADMIN -.->|Depends on| CORE
    CORE --> DB
    CORE --> RD
    CORE --> S3
```
* **도메인 격리**: `core-module`은 데이터베이스 및 데이터 스토리지 액세스 기술과 결합하고, 외부 서비스 모듈(`api-module`, `admin-module`)은 `core-module`을 라이브러리 형태로 의존하여 API 설계에만 집중할 수 있게 분리하였습니다.

---

## 7. API 엔드포인트 예제
### 🚗 렌터카 검색 API
* **요청 (Request)**
  * `GET /api/v1/cars/search`
  * Query parameters:
    * `pickup`: 대여 일시 (예: `2026-06-10T10:00:00`)
    * `returnTime`: 반납 일시 (예: `2026-06-12T10:00:00`)
    * `location`: 대여 도시 (예: `제주`, `서울`, `도쿄` 등)
    * `carType`: 차량 유형 (예: `전기 SUV`, `스포츠카` 등)
* **응답 (Response)**
  ```json
  {
    "status": "SUCCESS",
    "message": "렌터카 조회가 완료되었습니다.",
    "data": {
      "cars": [
        {
          "carId": 1,
          "modelName": "Audi A4",
          "carType": "럭셔리 세단",
          "licensePlate": "QA-227-CK",
          "dailyPrice": 75000,
          "location": "제주",
          "available": true
        }
      ],
      "totalCount": 1
    }
  }
  ```

---

## 8. 테스트
### 테스트 구동 방법
로컬 개발 환경의 변경 사항을 비즈니스 로직별로 검증하기 위해 JUnit 5 기반의 단위/통합 테스트 환경이 준비되어 있습니다.

```bash
# 전체 빌드 및 테스트 수행
./gradlew clean build

# 특정 모듈(예: api-module)의 테스트만 따로 실행
./gradlew :api-module:test
```
* **통합 테스트**: 테스트 컨테이너 또는 테스트용 인메모리 DB(H2) 설정을 활용해 실제 데이터 연동 검증을 안전하게 수행합니다.

---
