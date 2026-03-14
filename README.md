# PriceWatcher

> 사용자가 관심 상품을 저장하고 가격 변동을 추적하며, 목표가 도달 및 최저가 갱신 알림을 받을 수 있도록 설계한 패션 상품 가격 추적 백엔드 서비스

![Backend](https://img.shields.io/badge/Backend-Spring%20Boot%203.5.6-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.15.3-005571?logo=elasticsearch&logoColor=white)

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [핵심 기능](#핵심-기능)
- [리포지토리 구조](#리포지토리-구조)
- [기술 스택](#기술-스택)
- [주요 도메인](#주요-도메인)
- [사전 요구사항](#사전-요구사항)
- [실행 방법](#실행-방법)
- [테스트](#테스트)
- [주요 API](#주요-api)
- [환경 변수](#환경-변수)

---

## 프로젝트 소개

- PriceWatcher는 사용자가 패션 상품을 검색하고 저장한 뒤, 가격 변동을 지속적으로 추적할 수 있도록 만든 서비스

- 기본 흐름:
  - `상품 검색 -> 외부 상품 선택 저장 -> 워치리스트 등록 -> 가격 배치 갱신 -> 가격 이력 확인 -> 알림 수신`

- 내부 검색은 Elasticsearch 기반으로 동작하며, 외부 검색은 Naver Shopping API를 별도 경로로 분리하여 호출 비용을 통제하는 구조

- 가격 갱신은 Spring Batch 기반 2단계 구조로 구현하여, 1차 `(몰 + 브랜드)` 검색 후 미매칭 상품만 2차 개별 검색으로 처리하는 방식 적용

---

## 핵심 기능

- JWT Access / Refresh Token 기반 인증
- 내부 상품 검색 (`Elasticsearch` 우선, 실패 시 DB fallback)
- 외부 상품 검색 (`Naver Shopping API`)
- 상품 선택 저장 및 이미지 / 카테고리 / 가격 스냅샷 동시 저장
- 가격 배치 수동 실행 및 1차/2차 가격 갱신
- `price_history` 기반 가격 변동 추적
- 목표가 도달 / 최저가 갱신 / 가격 하락 알림
- 마이페이지 요약 조회
- 워치리스트 비교 그룹 생성 및 관리
- 동일 브랜드 / 유사 상품 추천

---

## 리포지토리 구조

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/pricewatch/
│   │   │   ├── domain/
│   │   │   │   ├── auth/
│   │   │   │   ├── category/
│   │   │   │   ├── email/
│   │   │   │   ├── notification/
│   │   │   │   ├── price/
│   │   │   │   ├── product/
│   │   │   │   ├── recommendation/
│   │   │   │   ├── task/
│   │   │   │   ├── user/
│   │   │   │   └── watchlist/
│   │   │   ├── global/
│   │   │   └── PricewatchApplication.java
│   │   └── resources/
│   │       ├── elasticsearch/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/                 # 단위/통합 테스트
├── docker-compose.yml
├── Dockerfile
├── build.gradle
└── README.md
```

---

## 기술 스택

- Backend
  - Java 21
  - Spring Boot 3.5.6
  - Spring Data JPA
  - Spring Security
  - JWT
  - Spring Batch
  - Spring Data Redis
  - Spring Data Elasticsearch
  - Spring Mail
  - Spring Retry
  - Redisson
  - MySQL

- Infra
  - Docker / Docker Compose
  - Elasticsearch 8.15.3
  - Redis 7

---

## 주요 도메인

- `auth`
  - JWT 기반 로그인 / 로그아웃 / 재발급

- `product`
  - 내부 검색, 외부 검색, 상품 선택 저장, 최신순 / 카테고리별 목록 조회

- `price`
  - 가격 배치 실행, 가격 히스토리 조회, 배치 결과 집계

- `task`
  - MySQL -> Elasticsearch 비동기 sync task 생성 / 처리 / 재시도

- `watchlist`
  - 워치리스트 등록 / 조회, 비교 그룹 생성 / 관리

- `notification`
  - 목표가 / 최저가 / 가격 하락 알림

- `recommendation`
  - 동일 브랜드 추천, 유사 상품 추천

- `category`
  - 카테고리 트리 조회 및 상품 카테고리 연결

- `email`
  - 이메일 Outbox 저장 및 발송

---

## 사전 요구사항

- Java 21
- Docker / Docker Compose
- 사용 포트
  - `8080` (Spring Boot)
  - `3307` (MySQL)
  - `6379` (Redis)
  - `9200` (Elasticsearch)

포트 충돌 시 기존 프로세스를 종료하거나 포트를 변경해야 함

---

## 실행 방법

### 1) Docker Compose로 실행

```bash
docker compose up -d
```

실행 대상:

- MySQL
- Redis
- Elasticsearch
- Spring Boot App

### 2) 로컬 개발 실행

1. 인프라 실행

```bash
docker compose up -d mysql redis elasticsearch
```

2. 애플리케이션 실행

Mac / Linux:

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

### 3) Elasticsearch 재색인

ES 인덱스 설정 / 동의어 변경 후에는 재색인이 필요

```http
POST /products/search-index/rebuild
```

---

## 테스트

Mac / Linux:

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```

---

## 주요 API

### 인증

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### 검색 / 저장

- `GET /products/search`
- `GET /products/search/external`
- `POST /products/select`
- `POST /products/search-index/rebuild`

### 상품 / 가격

- `GET /products/{id}`
- `GET /products/{id}/price-history?days=30`
- `GET /products/latest?page=0&size=20`
- `GET /products/categories/{categoryId}?page=0&size=20`

### 추천

- `GET /products/{id}/recommendations?limit=10`

### 배치 / 알림

- `POST /batch/price/run`
- `GET /notifications`

### 마이페이지 / 워치리스트

- `GET /users/me`
- `GET /users/me/summary`
- `GET /watchlist`
- `POST /watchlist/groups`
- `GET /watchlist/groups`
- `GET /watchlist/groups/{groupId}?days=30`
- `PATCH /watchlist/groups/{groupId}`
- `DELETE /watchlist/groups/{groupId}`
- `POST /watchlist/groups/{groupId}/items`
- `DELETE /watchlist/groups/{groupId}/items/{productId}`

### 카테고리

- `GET /categories`
- `GET /categories/{parentId}/children`

요청 예시는 `requests/` 폴더의 `.http` 파일 참고

---

## 환경 변수

기준 파일:

- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`
- `.env.dev`

대표 환경 변수:

```env
# DB
DB_URL=
DB_USERNAME=
DB_PASSWORD=

# Redis
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=

# Elasticsearch
ELASTICSEARCH_URIS=
PRODUCT_SEARCH_ES_ENABLED=
PRODUCT_SEARCH_ES_MIN_SCORE=

# JWT
JWT_SECRET=

# Naver Shopping API
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
NAVER_SEARCH_DISPLAY=
NAVER_DAILY_QUOTA=
NAVER_SEARCH_CACHE_TTL_SECONDS=

# Mail
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=

# App
ALLOWED_ORIGINS=
PRICE_BATCH_ENABLED=
PRICE_BATCH_CRON=
DB_SEARCH_SCORE_THRESHOLD=
PRICE_DROP_RATE_THRESHOLD=
```

---