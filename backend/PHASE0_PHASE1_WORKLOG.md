# Phase 0 + Phase 1 Worklog

## 작업 목적
- `yushinKim` 스타일 기준에 맞춰 인증 영역의 기본 컨벤션을 정렬한다.
- MVP 필수인 JWT Access/Refresh 흐름을 실제 동작 상태로 만든다.
- Refresh Token은 `HttpOnly Cookie`, Access Token은 `Authorization: Bearer` 방식으로 유지한다.

## 적용 일시
- 2026-03-03

## 변경 파일
1. `src/main/java/com/example/pricewatch/domain/auth/controller/AuthController.java`
2. `src/main/java/com/example/pricewatch/domain/auth/entity/RefreshToken.java`
3. `src/main/java/com/example/pricewatch/domain/auth/repository/RefreshTokenRepository.java`
4. `src/main/java/com/example/pricewatch/domain/auth/service/AuthService.java`
5. `src/main/java/com/example/pricewatch/global/config/SecurityConfig.java`
6. `src/main/java/com/example/pricewatch/global/exception/ErrorCode.java`
7. `src/main/resources/application.yml`

## 상세 변경 내역

### 1) AuthService 리팩토링 및 기능 완성
- 클래스 레벨에 `@Transactional(readOnly = true)` 적용.
- 쓰기 작업 메서드(`register`, `login`, `refresh`, `logout`)에 `@Transactional` 명시.
- 로그인 실패 에러를 `AUTH_REQUIRED`에서 `INVALID_LOGIN`으로 분리.
- Refresh Token 발급/회전 시 엔티티 `builder` 생성 방식으로 통일.
- `refresh(String rawRefreshToken)` 구현:
  - 쿠키 토큰 null/blank 검증
  - 해시 조회 + 미폐기 토큰 검증
  - 만료 시 폐기 후 `REFRESH_TOKEN_EXPIRED` 예외
  - 정상 시 기존 토큰 즉시 폐기(revoke)
  - 새 refresh token 재발급(회전) + 새 access token 발급
- `logout(String rawRefreshToken)` 구현:
  - 쿠키 토큰이 있으면 해당 토큰만 폐기
  - 토큰이 없거나 이미 폐기 상태면 no-op (멱등성)

### 2) AuthController API 완성
- `/auth/login`:
  - 서비스 결과로 받은 refresh token을 HttpOnly 쿠키로 설정
- `/auth/refresh`:
  - 쿠키에서 refresh token 수신
  - 토큰 회전 결과를 다시 쿠키에 설정
  - body에는 새 access token 반환
- `/auth/logout`:
  - 쿠키 refresh token 폐기 처리 호출
  - 만료 쿠키(`maxAge=0`) 내려서 브라우저 쿠키 제거
- 쿠키 정책 공통화:
  - `createRefreshCookie`, `clearRefreshCookie` 메서드로 일원화
  - `sameSite=Lax`, `path=/auth`, `httpOnly=true`
  - `secure`는 환경 설정으로 제어

### 3) RefreshToken 엔티티 개선
- `isExpired(LocalDateTime now)` 메서드 추가.
- 기존 `revokeNow`, `isRevoked`와 조합해 토큰 상태 판단 로직 단순화.

### 4) RefreshTokenRepository 확장
- `findByTokenHashAndRevokedAtIsNull(...)` 추가.
- refresh/ logout 시 “활성 토큰” 조회에 사용.

### 5) ErrorCode 확장
- `INVALID_LOGIN` 추가.
- `REFRESH_TOKEN_EXPIRED` 추가.
- 인증 실패 원인을 더 정확히 분기하도록 수정.

### 6) SecurityConfig 보강
- CORS 설정 추가 (`CorsConfigurationSource` 빈 등록).
- 허용 오리진은 `app.cors.allowed-origins` 값(콤마 구분) 사용.
- `allowCredentials=true` 설정으로 쿠키 기반 인증 흐름 지원.
- `Set-Cookie`, `Authorization` exposed header 추가.

### 7) application.yml 설정 추가
- `app.auth.refresh-cookie-name`
- `app.auth.refresh-cookie-secure`
- 환경별로 쿠키 이름/secure 여부를 조정할 수 있도록 확장.

## 동작 시나리오 (최종)
1. 로그인 성공
   - Body: Access Token 반환
   - Header(Set-Cookie): Refresh Token(HttpOnly)
2. 토큰 재발급
   - Request Cookie: Refresh Token
   - 서버: 기존 refresh revoke + 새 refresh 발급
   - Body: 새 Access Token
   - Header(Set-Cookie): 새 Refresh Token
3. 로그아웃
   - Request Cookie: Refresh Token
   - 서버: 토큰 revoke
   - Header(Set-Cookie): 쿠키 삭제

## 검증 결과
- 실행 커맨드: `./gradlew.bat test`
- 결과: 성공 (Exit code 0)

## yushinKim 스타일 관점 반영 포인트
- 서비스 트랜잭션 경계를 명확히 분리.
- 예외 코드 세분화로 비즈니스 에러를 명시적으로 표현.
- 컨트롤러-서비스 책임 분리(쿠키 I/O는 컨트롤러, 토큰 상태 변경은 서비스).

## 다음 단계 제안 (Phase 2 연결)
1. `Naver API Client + 검색 캐시 + quota + single-flight` 구현
2. `category1~4 트리 upsert`와 상품 leaf category 연결
3. `externalKey(link normalize + sha256)` 정책을 저장 로직에 강제 적용

---

## 추가 스타일 통일 작업 (요청 반영)

### 기준
1. DTO는 `record`만 사용 (`@Builder` 사용 금지)
2. 엔티티 생성은 `builder`로 통일
3. 주석은 복잡한 맥락에만 최소한으로 유지

### 반영 내역
1. 엔티티 정적 팩토리 제거
   - `RefreshToken.create`, `Category.of`, `Notification.of`, `PriceHistory.of`, `Watchlist.of` 삭제
2. 서비스에서 엔티티 생성 시 builder 직접 사용
   - `AuthService`, `WatchlistService` 반영
3. 자명한 주석 정리
   - `AuthController`, `AuthService`, `SecurityConfig`, `ResponseDto` 등 기본 설명성 주석 축소
4. 점검 결과
   - DTO에서 `class`/`@Builder` 미사용 확인
   - Entity에서 정적 팩토리(`of/from/create`) 미사용 확인
