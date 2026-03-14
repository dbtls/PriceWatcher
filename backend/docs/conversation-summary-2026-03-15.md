# PriceWatcher Detailed Build Notes

## 문서 목적

이 문서는 긴 대화 세션에서 진행한 구현 내용, 설계 의사결정, 트러블슈팅, 향후 개선 포인트를 포트폴리오와 다음 세션 인수인계에 모두 활용할 수 있도록 상세히 정리한 문서다.

초기 요약 문서는 "무엇을 만들었는가" 중심이었다면, 이 문서는 아래 질문에 답할 수 있도록 확장했다.

- 왜 이 구조를 선택했는가
- 어떤 문제가 실제로 발생했고 어떻게 풀었는가
- 어떤 구조는 의도적으로 채택했고 어떤 구조는 의도적으로 포기했는가
- 포트폴리오에 적을 때 어떤 포인트를 강조해야 하는가
- 다음 리팩토링 지점은 어디인가

---

## 프로젝트 한 줄 정의

PriceWatcher는 사용자가 관심 상품을 저장하고, 가격 변동을 추적하며, 목표가 도달/최저가 갱신/가격 하락 알림을 받도록 설계한 패션 상품 가격 추적 백엔드 서비스다.

이 프로젝트의 핵심 가치는 단순 상품 CRUD가 아니라 아래 네 축에 있다.

1. 외부 쇼핑 API 의존 비용을 통제하는 검색/배치 구조
2. MySQL을 원본으로 두고 Elasticsearch를 검색 인덱스로 분리한 비동기 동기화 구조
3. Spring Batch 기반 가격 갱신과 일자별 가격 스냅샷 저장 구조
4. 검색, 워치리스트, 비교 그룹, 알림, 추천으로 이어지는 사용자 중심 탐색 흐름

---

## 전체 구현 맥락

처음부터 해결하려고 했던 문제는 비교적 명확했다.

- 사용자가 브랜드/상품명을 검색해 상품을 찾을 수 있어야 한다.
- 내부 DB에 없는 상품은 외부 검색 결과를 보고 선택 저장할 수 있어야 한다.
- 저장된 상품은 이후 가격 배치로 주기적으로 갱신되어야 한다.
- 사용자는 워치리스트에 상품을 담고 가격 변화를 추적할 수 있어야 한다.
- 가격 변화는 일자별로 기록되어 차트/히스토리로 보여줄 수 있어야 한다.
- 목표가 이하 도달, 최저가 갱신, 일정 하락률 도달 시 알림을 받을 수 있어야 한다.
- 검색 품질은 단순 DB `contains`를 넘어서야 한다.
- 패션 상품이라는 도메인 특성상 동일하거나 유사한 상품을 비교하고 추천할 수 있어야 한다.

이 요구사항을 다루는 과정에서, 구현 자체보다 구조 선택이 중요해졌다. 특히 아래 항목들이 핵심 설계 포인트였다.

- 내부 검색과 외부 검색을 어디서 분리할 것인가
- 외부 API 호출량을 어떻게 통제할 것인가
- 배치는 상품 단위로 돌릴 것인가, 브랜드/몰 단위로 묶을 것인가
- ES를 단순 검색 엔진으로 붙일지, 시스템의 원본 저장소처럼 쓸지
- 멀티몰 동일 상품 자동 그룹핑을 얼마나 공격적으로 할 것인가
- 추천을 행동 기반으로 갈지, 상품 메타데이터 기반으로 갈지

---

## 1. 인증 / JWT / Refresh Token 구조

### 현재 구조

- 로그인 성공 시 `accessToken`은 응답 body로 반환
- `refreshToken`은 HttpOnly cookie로 `/auth` 경로에 저장
- `POST /auth/refresh` 엔드포인트로 access token 재발급
- access token 만료 15분
- refresh token 만료 14일

### 관련 파일

- `src/main/java/com/example/pricewatch/domain/auth/controller/AuthController.java`
- `src/main/java/com/example/pricewatch/domain/auth/service/AuthService.java`
- `src/main/java/com/example/pricewatch/global/security/JwtTokenProvider.java`
- `src/main/java/com/example/pricewatch/global/security/JwtAuthenticationFilter.java`
- `src/main/resources/application.yml`

### 설계 의도

인증 구조는 비교적 전형적이지만, 포트폴리오에서는 "왜 이런 토큰 분리 구조를 썼는가"를 설명하는 것이 중요하다.

- access token은 짧은 수명으로 탈취 리스크 축소 목적
- refresh token은 HttpOnly cookie에 저장하여 JS 직접 접근 차단 목적
- access token 재발급 경로를 별도 엔드포인트로 분리하여 프론트의 자동 재시도 흐름과 연결하기 위한 목적

### 실제 논의된 문제

사용자가 "몇 분 지나면 로그인이 풀린다"는 문제를 제기했다. 이때 백엔드 쪽 구현을 점검한 결과, 재발급 API 자체는 이미 존재했다.

따라서 현재 가장 유력한 원인은 아래로 판단했다.

- 프론트가 401 발생 시 `/auth/refresh`를 자동 호출하지 않음
- `withCredentials` 또는 `credentials: include` 설정이 빠져 refresh cookie가 전송되지 않음

### 포트폴리오 포인트

이 부분은 단순 JWT 적용보다는 "access/refresh 분리, HttpOnly cookie 적용, 자동 재발급 흐름 분리" 정도로 요약하는 것이 적절하다.

---

## 2. 검색 구조: 내부 검색과 외부 검색의 분리

### 현재 검색 API

- `GET /products/search`
  - 내부 검색 전용
  - ES 우선
  - 실패 시 DB fallback
  - 현재는 개별 상품 리스트 반환

- `GET /products/search/external`
  - 네이버 쇼핑 외부 검색 전용

### 관련 파일

- `src/main/java/com/example/pricewatch/domain/product/service/ProductSearchService.java`
- `src/main/java/com/example/pricewatch/domain/product/dto/ProductSearchRes.java`
- `src/main/java/com/example/pricewatch/domain/product/dto/ProductSummaryRes.java`
- `src/main/java/com/example/pricewatch/domain/product/controller/ProductController.java`

### 왜 분리했는가

초기에는 "사용자가 검색하면 내부 DB 결과와 외부 API 결과를 한 번에 같이 보여주면 편하지 않을까"라는 방향을 검토했다. 하지만 이 구조는 곧 운영 비용 문제와 충돌했다.

문제는 아래와 같았다.

- 내부 DB에 이미 존재하는 상품도 매 검색마다 외부 API를 호출하게 됨
- 검색 트래픽이 늘수록 외부 API 일일 quota를 빠르게 소모하게 됨
- 검색 UX와 외부 API 비용이 강하게 결합됨

따라서 검색을 아래처럼 역할 분리했다.

- 기본 검색은 내부 검색으로 해결
- 외부 검색은 사용자가 정말 필요할 때만 별도 호출

이 구조는 단순한 API 분리가 아니라, 검색 비용 통제 전략이다.

### DB fallback 점수 구조

ES가 실패할 경우를 대비해 DB fallback도 유지했다. 다만 이 로직은 "검색 품질의 주력"이 아니라 "장애 시 최소 검색 가능성 보장" 정도의 역할로 남겼다.

DB fallback은 대략 아래와 같은 점수 체계를 사용했다.

- 제목에 검색어 전체 포함: 높은 가중치
- 브랜드에 검색어 전체 포함: 높은 가중치
- 검색 토큰의 브랜드/제목 포함 여부: 중간 가중치

이 구조를 유지한 이유는 두 가지다.

- ES가 불안정하거나 재색인 직후 상태가 꼬였을 때 fallback 역할
- ES 결과 품질을 비교할 기준선 역할

### ES 쪽 튜닝 포인트

검색 품질을 높이기 위해 ES 쪽에는 아래를 적용했다.

- `match_phrase` + `match` 조합
- 브랜드, 제목, 카테고리 가중치 차등
- `min_score` 적용
- 색상처럼 약한 토큰만 맞는 결과를 제외하기 위한 token gate
- 한글/영문 혼합 검색을 위한 synonym 사전

관련 파일:

- `src/main/java/com/example/pricewatch/domain/task/service/ProductSearchIndexService.java`
- `src/main/resources/elasticsearch/product-search-synonyms.txt`

### 실제 겪은 문제

검색 품질을 높이는 과정에서 단순히 "더 많이 나오게" 하면 오히려 품질이 나빠졌다. 대표적인 사례는 아래였다.

- 검색어: `VTG 페이디드 후드 집업 블랙`
- 결과에 색상만 비슷한 데님 쇼츠 상품이 섞여 나옴

이 문제를 해결하기 위해 아래 조치를 적용했다.

- ES `min_score` 추가
- 색상 토큰처럼 약한 토큰만 맞는 결과 제외
- 강한 토큰이 브랜드/제목/카테고리 중 하나에 실제 포함될 때만 통과

즉, 검색 품질 문제를 "사전만 늘리면 해결된다"로 보지 않고, 관련도 계산과 결과 게이트를 따로 두는 방식으로 풀었다.

---

## 3. Elasticsearch 도입과 MySQL - ES 비동기 동기화

### 현재 구조 요약

MySQL을 원본 저장소(source of truth)로 유지하고, Elasticsearch는 검색 전용 read model로 사용한다.

흐름은 아래와 같다.

1. 상품 저장/수정/배치 갱신
2. MySQL 커밋
3. ES 동기화 task 생성
4. 비동기 리스너가 ES upsert/delete 실행
5. 성공 시 task 성공 마킹
6. 실패 시 스케줄러 재시도

### 관련 파일

- `src/main/java/com/example/pricewatch/domain/task/service/ProductSearchIndexService.java`
- `src/main/java/com/example/pricewatch/domain/task/service/ProductSearchDocument.java`
- `src/main/java/com/example/pricewatch/domain/task/service/ProductSearchSyncService.java`
- `src/main/java/com/example/pricewatch/domain/task/service/DbEventListener.java`
- `src/main/java/com/example/pricewatch/domain/task/service/PendingSyncScheduler.java`
- `src/main/java/com/example/pricewatch/domain/task/entity/AsyncTask.java`
- `src/main/java/com/example/pricewatch/domain/task/service/AsyncTaskTxService.java`

### 왜 이렇게 설계했는가

ES를 붙일 때 가장 위험한 선택은 ES를 원본처럼 다루는 것이다. 하지만 이 프로젝트에서 상품 원본, 가격, 알림, 배치는 모두 MySQL 기준으로 정합성이 유지되어야 했다.

따라서 아래 원칙을 명확히 세웠다.

- MySQL만이 정답 데이터
- ES는 검색 성능 향상을 위한 파생 인덱스
- ES 반영이 늦더라도 MySQL 정합성은 깨지지 않아야 함

이 원칙을 지키기 위해 async task 구조를 택했다.

### Redis pending을 쓰지 않은 이유

초기에는 다른 프로젝트(IgLoo) 구조를 참고해 Redis pending 개념도 검토했다. 그러나 결국 제거했다.

이유는 아래와 같다.

- 현재 프로젝트의 정보 원천은 Redis가 아니라 MySQL
- 실패 task 상태도 이미 DB task row로 남김
- 재시도 스케줄러도 DB task만 보면 충분

즉 Redis pending은 이 프로젝트에선 필수 상태가 아니라 보조 상태에 불과했다. 결국 구조를 단순화하기 위해 DB task 중심으로 정리했다.

### 실제 리스너 구조

`DbEventListener`는 아래 흐름으로 동작한다.

- `@Async("DBTaskExcutor")`로 비동기 실행
- ES upsert/delete 수행
- 성공 시 `afterCommit`에서 task 성공 마킹
- `@Retryable(maxAttempts = 3)`로 일시적 실패 재시도

코드상 의도는 "ES 실패가 MySQL 원본을 오염시키지 않도록 하되, 추적 가능한 실패 상태를 남기는 것"이다.

### 포트폴리오에서 강조할 포인트

이 부분은 단순 "ES 붙였다"가 아니라 아래처럼 써야 강하다.

- MySQL을 source of truth로 유지한 read model 분리 구조
- 동기식 색인 대신 task 기반 eventually consistent 구조
- 실패 이력 추적 및 재시도 가능한 검색 인덱스 반영 파이프라인

### 남아 있는 리스크

- ES 검색 응답 total hit count를 정확히 내려주지 않고 있음
- 인덱스 재생성 시 운영 중 동시 검색 영향에 대한 더 정교한 전략은 없음
- task queue가 매우 커질 경우 backpressure 고려 필요

---

## 4. 상품 저장 플로우와 이미지/카테고리/가격 히스토리 결합

### 현재 구조

외부 검색 결과 중 사용자가 특정 상품을 선택 저장하면, 단순 `products` row 하나 저장으로 끝나지 않는다.

현재 `ProductService.select(...)`는 아래를 함께 처리한다.

1. 상품 row 저장 또는 갱신
2. 이미지 URL 저장
3. 카테고리 트리 저장/연결
4. 오늘 날짜 기준 `price_history` upsert
5. ES sync upsert 발행

관련 파일:

- `src/main/java/com/example/pricewatch/domain/product/service/ProductService.java`
- `src/main/java/com/example/pricewatch/domain/product/dto/ProductSelectReq.java`
- `src/main/java/com/example/pricewatch/domain/product/dto/ProductSelectRes.java`

### 왜 저장 시점에 `price_history`도 남기게 했는가

처음에는 가격 히스토리는 배치가 돌 때만 쌓였다. 하지만 곧 문제가 드러났다.

- 오전 배치가 이미 끝난 뒤 사용자가 새 상품을 저장
- 그 상품은 오늘 가격 이력이 비어버림
- 즉 가격 추적 서비스인데 "오늘 저장했지만 오늘의 가격 기록이 없음"이라는 어색한 상태 발생

이를 해결하기 위해 선택 저장 시점에도 당일 스냅샷을 즉시 기록하게 바꿨다.

이 결정은 단순 편의가 아니라 데이터 연속성 보장 전략이다.

---

## 5. 카테고리 구조: 네이버 원본 카테고리 기반 적재

### 현재 상태

현재 카테고리는 네이버 쇼핑 API 응답의 `category1~4`를 그대로 계층형으로 누적 저장하는 구조다.

카테고리 조회 API는 이미 존재한다.

- `GET /categories`
- `GET /categories/{parentId}/children`

관련 파일:

- `src/main/java/com/example/pricewatch/domain/category/controller/CategoryController.java`
- `src/main/java/com/example/pricewatch/domain/category/service/CategoryService.java`
- `src/main/java/com/example/pricewatch/domain/category/dto/CategoryNodeRes.java`

### 실제 발생한 문제

패션 서비스로 설계했는데, 카테고리 트리에 패션 외 이상한 분류가 섞여 들어왔다.

원인은 명확했다.

- 네이버 검색 결과 자체가 패션 외 상품을 일부 섞어서 반환
- 저장 시 `category1~4`를 그대로 누적
- 그 결과 서비스 전용 카테고리 체계가 아니라 "네이버 검색 결과 누적 카테고리"가 되어 버림

### 현재 판단

아직 코드 수정은 하지 않았지만, 이후에는 아래 방향이 필요하다.

- 저장 시 패션 카테고리 whitelist 적용
- `/categories` 조회 시 패션 계열만 반환

즉 카테고리를 "외부 플랫폼 분류를 그대로 들여오는 문제"에서 "서비스 내부 탐색 체계로 정제하는 문제"로 바라볼 필요가 있다.

### 네이버 카테고리 문서 관련 결론

대화 과정에서 "네이버 쇼핑 전체 카테고리 마스터 문서가 있는가"를 검토했다.

확인 결과:

- 쇼핑 검색 API 응답에는 `category1~4`가 있음
- 하지만 별도의 완전한 카테고리 마스터 사전/코드 API는 찾지 못함

따라서 현실적인 전략은 아래다.

- 검색 응답을 통해 실제 등장 카테고리를 수집
- 내부 카테고리 정책으로 정제
- 필요 시 패션 전용 내부 매핑 테이블로 승격

---

## 6. 메인페이지용 목록 API

### 추가한 API

- `GET /products/latest?page=0&size=20`
- `GET /products/categories/{categoryId}?page=0&size=20`

관련 파일:

- `src/main/java/com/example/pricewatch/domain/product/service/ProductBrowseService.java`
- `src/main/java/com/example/pricewatch/domain/product/dto/ProductListRes.java`
- `src/main/java/com/example/pricewatch/domain/product/repository/ProductRepository.java`
- `src/main/java/com/example/pricewatch/domain/product/controller/ProductController.java`
- `requests/product-browse.http`

### 설계 이유

검색 API만으로는 메인페이지를 구성하기 어렵다. 메인페이지는 아래 요구를 별도로 가진다.

- 최신 저장 상품 피드
- 카테고리 탭/필터 기반 상품 탐색

따라서 검색과 browse를 명확히 분리했다.

- search: 사용자가 키워드를 입력한 적극적 탐색
- browse: 사용자가 카테고리/최신순으로 훑어보는 탐색

이 분리는 프론트 UX뿐 아니라 백엔드 쿼리 책임 분리에도 도움을 준다.

---

## 7. 가격 배치 구조: 1차 브랜드/몰 검색 + 2차 개별 검색

### 현재 배치 개요

배치는 `POST /batch/price/run`으로 수동 실행 가능하며, 구조는 두 단계다.

1. 1차 배치
   - `(mall + brand)` 기준 그룹 검색
   - 하나의 외부 API 호출로 여러 상품을 한 번에 갱신하려는 목적

2. 2차 배치
   - 1차에서 갱신되지 않은 상품만 개별 검색

관련 파일:

- `src/main/java/com/example/pricewatch/domain/price/service/PriceRefreshBatchService.java`
- `src/main/java/com/example/pricewatch/domain/price/batch/PriceRefreshJobConfig.java`
- `src/main/java/com/example/pricewatch/domain/price/batch/PriceRefreshJobRunner.java`
- `src/main/java/com/example/pricewatch/domain/price/dto/PriceRefreshBatchSummary.java`
- `src/main/java/com/example/pricewatch/domain/price/controller/PriceBatchController.java`

### 왜 이렇게 설계했는가

상품 단위로 매번 외부 API를 호출하면 호출 수가 상품 수와 정비례한다. 이건 대량 데이터에서 곧바로 quota 문제로 이어진다.

반면 패션 상품은 같은 브랜드, 같은 몰 조합 안에서 검색 결과가 묶여 나오는 경향이 있다. 이를 활용해 아래 구조를 만들었다.

- 1차는 `(몰 + 브랜드)`로 넓게 검색
- 결과 안에서 `naverProductId` 또는 `externalKey`로 개별 상품을 매칭
- 매칭 안 된 것만 2차 개별 검색

이 구조의 장점:

- 외부 API 호출 수 절감
- 대량 상품 갱신에 더 적합
- quota 내 처리량 증대

즉 배치를 "상품마다 검색"이 아니라 "먼저 넓게, 나중에 좁게"로 설계한 것이다.

### 배치와 ES의 역할 분리

배치는 ES 검색을 사용하지 않는다. 의도적으로 그렇다.

- 배치 입력은 MySQL + 네이버 API
- ES는 사용자 검색용 인덱스

배치가 ES를 기준으로 상품을 찾기 시작하면 검색 인덱스 상태가 원본처럼 쓰이게 된다. 이는 원본/파생 저장소 역할을 뒤섞는 일이므로 피했다.

### 배치 결과 처리

가격 갱신에 성공하면 아래가 연쇄적으로 수행된다.

- `products.price`, `lastSeenAt`, `refreshStatus` 갱신
- `price_history` 저장
- 워치리스트 기준 알림 생성
- ES sync task 발행

### 배치 수동 실행 응답

배치 응답에는 아래가 포함되도록 확장했다.

- first pass 처리 그룹 수
- first pass 갱신 수
- second pass 처리 후보 수
- second pass 갱신 수
- 실패 수
- 목표가/최저가/하락률 알림 수
- quota 소진 여부
- 1차/2차/총 네이버 API 호출 수

이 응답은 단순 디버깅용이 아니라 운영 관측성 강화 목적이다.

---

## 8. 배치 관련 실제 트러블슈팅

### 8-1. `Scope 'step' is not active` 문제

배치 실행 초기에 아래 오류가 발생했다.

- `Scope 'step' is not active`

원인은 step scope bean 사용 방식과 실제 실행 컨텍스트가 맞지 않았기 때문이다.

해결 방향:

- `@StepScope` 제거
- `jobParameters`는 step 내부에서 직접 읽는 구조로 변경

이 문제를 통해 얻은 교훈은 "Spring Batch를 tasklet 방식으로 단순하게 쓸수록 오히려 bean scope 문제를 덜어낼 수 있다"는 점이었다.

### 8-2. `price_history.price` null insert 문제

배치가 실제로 돌기 시작한 뒤, `price_history` insert에서 아래 문제가 발생했다.

- `Column 'price' cannot be null`

원인은 신규 `PriceHistory` 생성 시 `capturedAt`, `createdAt`만 세팅하고 `price`를 누락한 구현 문제였다.

해결:

- `savePriceHistory(...)`에서 신규 생성 시 `latestPrice`를 즉시 주입

추가적으로 이 예외 이후 Hibernate session flush 과정에서 assertion failure가 연쇄 발생하는 문제도 같이 드러났다. 즉, 배치에서 "작은 누락 하나가 전체 step 실패로 이어지는" 구조적 취약점도 확인할 수 있었다.

### 8-3. 저장 시점의 당일 히스토리 누락

배치가 정상 동작해도, 배치 이후 새로 저장된 상품은 당일 이력이 비는 문제가 있었다.

해결:

- `ProductService.select(...)`에서 저장 시점 당일 `price_history` upsert 추가

이 변경으로 아래가 가능해졌다.

- 배치 전 저장 상품: 당일 이력 존재
- 배치 후 저장 상품: 당일 이력 존재
- 같은 날 배치 재실행 시 기존 row 갱신

즉 데이터 연속성이 더 자연스러워졌다.

---

## 9. 배치 구조에 대한 판단: tasklet vs chunk

### 현재 구조

현재는 tasklet 중심 구조다.

- 1차 step에서 많은 그룹을 한 번에 처리
- 2차 step에서 많은 상품을 한 번에 처리

### 장점

- 구현이 빠름
- 흐름이 단순함
- MVP 단계에서 로직 검증에 유리

### 단점

- 트랜잭션 단위가 큼
- 중간 실패 시 롤백 범위가 큼
- 대량 데이터일수록 불리
- "실패한 일부만 건너뛰고 계속 진행"이 어렵다

### 왜 chunk 전환을 논의했는가

대화 중 아래 요구가 명확하게 제기됐다.

- 배치가 도중 일부 상품에서 실패해도 전체가 롤백되면 안 됨
- 실패한 chunk만 실패 처리하고 다음 작업으로 넘어가야 함

이 요구를 만족하려면 chunk 기반 전환이 필요하다는 결론에 도달했다.

다만 현재는 기능 완성 우선으로 tasklet 구조를 유지하고 있고, 이는 포트폴리오에서 "향후 개선 과제"로 적기 적절하다.

포트폴리오 서술 예시:

- 현재는 tasklet 기반 2단계 배치 구조를 구현
- 향후 대량 데이터 운영을 위해 chunk 기반 partial commit / skip / retry 정책으로 전환 예정

---

## 10. 알림 구조

### 현재 알림 조건

배치 가격 갱신 시 워치리스트를 조회해 아래 조건에 맞으면 알림 생성

- 목표가 이하 도달
- 최저가 갱신
- 일정 비율 이상 가격 하락

관련 파일:

- `src/main/java/com/example/pricewatch/domain/notification/service/NotificationService.java`
- `src/main/java/com/example/pricewatch/domain/price/service/PriceRefreshBatchService.java`

### 설계 포인트

알림은 "상품 가격이 변했다"는 이벤트를 사용자에게 그대로 던지는 것이 아니라, 사용자가 저장해 둔 관심 기준과 연결되어야 한다.

따라서 알림은 product 단위 이벤트가 아니라 watchlist 문맥에서 판단된다.

이 구조는 향후 아래 확장에도 유리하다.

- 이메일 outbox
- push notification
- 알림 센터 읽음 처리

---

## 11. 검색 그룹핑 시도와 포기

### 왜 그룹핑을 시도했는가

멀티몰 가격 비교 UX를 만들고 싶었기 때문이다. 예를 들어 아래 같은 경우를 하나의 카드로 묶고 싶었다.

- 무신사의 동일 상품
- 29CM의 동일 상품

처음에는 검색 결과 자체를 그룹으로 묶고, 프론트에서 그룹 상세를 열어 몰별 가격을 보여주는 구조를 시도했다.

### 왜 실패했는가

패션 상품명은 매우 미묘하다.

예시:

- `selvedge denim pants deep indigo`
- `selvedge denim pants indigo`

- `heavyweight pocket tee mustard`
- `heavyweight pocket l/s tee mustard`

브랜드와 대부분 토큰이 같아도 실제로는 다른 상품일 수 있다.

이 문제를 규칙으로 계속 보정하려고 하면 아래 상황이 발생한다.

- 색상 변형
- 슬리브 길이
- fit
- washed/faded/deep/light
- model code
- 시즌 표현

즉 규칙이 끝없이 늘어난다.

### 내린 결론

자동 그룹핑은 "안 묶는 실수"보다 "잘못 묶는 실수"가 훨씬 치명적이었다.

따라서 최종적으로 아래 결정을 했다.

- 검색 결과 자동 그룹핑 제거
- 검색은 개별 상품 기준으로 단순화
- 비교는 워치리스트에서 사용자가 직접 그룹을 만든다
- 유사성 탐색은 추천 기능으로 이동

### 실제 제거한 코드

삭제:

- `ProductSearchGroupRes`
- `ProductSearchGroupingService`
- `ProductGroupDetailReq`
- `ProductGroupDetailOfferRes`
- `ProductGroupDetailRes`
- `ProductGroupDetailService`
- `requests/product-group-detail.http`
- `/products/search/groups/detail`

이 결정은 기능 축소가 아니라, "시스템이 자동 판별해야 할 문제"를 "사용자가 제어 가능한 비교 문제"로 역할 전환한 것이다.

---

## 12. 추천 기능: 그룹핑 대신 추천으로 문제 재정의

### 현재 구현된 추천 API

- `GET /products/{id}/recommendations?limit=10`

응답:

- `brandSimilarProducts`
- `similarProducts`

관련 파일:

- `src/main/java/com/example/pricewatch/domain/recommendation/controller/RecommendationController.java`
- `src/main/java/com/example/pricewatch/domain/recommendation/service/RelatedProductService.java`
- `src/main/java/com/example/pricewatch/domain/recommendation/dto/ProductRecommendationsRes.java`

### 왜 추천으로 방향을 바꿨는가

검색 결과에서 자동 그룹핑을 강하게 적용하면 오탐이 치명적이었다. 반면 추천은 "비슷한 상품을 보여주는 기능"이므로 약간의 오차를 허용하기 훨씬 쉽다.

즉 같은 유사도 계산이라도:

- 검색 그룹핑: 잘못 묶이면 UX가 깨짐
- 추천: 조금 애매해도 탐색 보조로 수용 가능

따라서 유사도 계산 로직은 그룹핑보다 추천에 더 적합하다고 판단했다.

### 같은 브랜드 추천 로직

같은 브랜드 추천은 아래 순서가 앞서도록 점수를 설계했다.

1. 같은 제목의 다른 몰 상품
2. 같은 모델코드 공유
3. 같은 core token 집합
4. 같은 색상
5. 같은 카테고리
6. 토큰 overlap

즉 같은 브랜드 안에서는 "같은 제품의 다른 몰" 혹은 "같은 제품의 색상/변형"이 먼저 뜨도록 설계했다.

### 브랜드 무관 유사 추천 로직

브랜드 무관 추천은 아래 신호를 사용한다.

- 제목 토큰 유사도
- 카테고리 일치 여부
- 색상 일치 여부

이 추천은 "같은 브랜드가 아니더라도 비슷한 제품군을 보여준다"는 목적이다.

예:

- 검정 티셔츠
- 검정 긴팔 티셔츠
- 무지 티셔츠

### 이 추천 구조가 포트폴리오에서 의미 있는 이유

이 기능은 단순 추천이 아니라 "검색 자동 그룹핑 실패를 추천 문제로 재정의한 설계 판단"이라는 점에서 의미가 있다.

즉 아래처럼 서술할 수 있다.

- 자동 그룹핑의 오탐 문제를 해결하기 위해 검색은 개별 상품 중심으로 단순화
- 대신 상세 진입 시 동일 브랜드/유사 상품 추천을 제공하여 탐색 UX 유지

---

## 13. 워치리스트 비교 그룹: 자동 그룹핑 대신 사용자 정의 비교

### 왜 만들었는가

자동 그룹핑을 제거한 뒤에도 비교 니즈는 여전히 존재했다. 따라서 비교를 시스템 자동 판별이 아니라 사용자의 명시적 행위로 옮겼다.

### 현재 구조

새로 추가된 도메인:

- `WatchlistGroup`
- `WatchlistGroupItem`

관련 파일:

- `src/main/java/com/example/pricewatch/domain/watchlist/entity/WatchlistGroup.java`
- `src/main/java/com/example/pricewatch/domain/watchlist/entity/WatchlistGroupItem.java`
- `src/main/java/com/example/pricewatch/domain/watchlist/service/WatchlistGroupService.java`

API:

- `POST /watchlist/groups`
- `GET /watchlist/groups`
- `GET /watchlist/groups/{groupId}?days=30`
- `PATCH /watchlist/groups/{groupId}`
- `DELETE /watchlist/groups/{groupId}`
- `POST /watchlist/groups/{groupId}/items`
- `DELETE /watchlist/groups/{groupId}/items/{productId}`

### 설계 의도

검색 단계의 자동 그룹핑은 시스템이 책임져야 했다. 반면 워치리스트 비교 그룹은 사용자가 직접 비교 집합을 만들기 때문에 훨씬 유연하다.

장점:

- 같은 브랜드/다른 몰 비교 가능
- 아예 다른 브랜드 상품도 비교 가능
- 비교 기준을 사용자가 직접 통제
- 자동 판별 오탐 문제 제거

### 현재 정책

현재는 그룹에 넣을 수 있는 상품을 "이미 워치리스트에 담긴 상품"으로 제한했다.

이유:

- 사용자가 관심 있다고 명시한 상품만 비교 대상으로 삼기 위함
- 비교 그룹이 곧 관심 집합의 부분집합이 되도록 하기 위함

### 그룹 상세 구조

그룹 상세 조회 시 각 아이템에 대해 아래를 함께 제공한다.

- 현재가
- 목표가
- 최저가
- 상품 URL
- 가격 히스토리

즉 프론트에서는 그룹 상세만 받아도 비교표 + 차트 UI를 충분히 그릴 수 있다.

---

## 14. 마이페이지 요약 API

### 추가한 API

- `GET /users/me/summary`

### 제공 정보

- 프로필 기본 정보
- 워치리스트 수
- 비교 그룹 수
- 읽지 않은 알림 수
- 목표가 도달 수
- 최근 가격 하락 상위 5개

관련 파일:

- `src/main/java/com/example/pricewatch/domain/user/service/UserService.java`
- `src/main/java/com/example/pricewatch/domain/user/controller/UserController.java`
- `src/main/java/com/example/pricewatch/domain/user/dto/MyPageSummaryRes.java`
- `src/main/java/com/example/pricewatch/domain/user/dto/MyPageRecentDropRes.java`

### 왜 필요한가

마이페이지는 단순 프로필 조회가 아니라, 사용자의 상품 추적 활동을 한 화면에 요약해야 한다. 따라서 summary API는 단순 user row 조회가 아니라 여러 도메인의 집계 API다.

이 API는 향후 프론트에서 아래 영역을 구성하는 기반이 된다.

- 대시보드 카드
- 최근 변동 상품
- 알림 진입
- 비교 그룹 진입

---

## 15. 가격 히스토리와 그래프 API에 대한 역할 분리

### 현재 API

- `GET /products/{id}/price-history?days=30`

관련 파일:

- `src/main/java/com/example/pricewatch/domain/price/controller/PriceController.java`
- `src/main/java/com/example/pricewatch/domain/price/service/PriceHistoryService.java`
- `src/main/java/com/example/pricewatch/domain/price/dto/PriceHistoryItemRes.java`

### 구조 판단

대화에서 "상품 가격 추이를 그래프로 그리고 싶다"는 요구가 있었고, 여기서 역할을 명확히 분리했다.

- 백엔드 역할: 시계열 데이터 제공
- 프론트 역할: 차트 렌더링

이 판단은 중요하다. 백엔드는 그래프를 그리지 않지만, 프론트가 그래프를 쉽게 그릴 수 있는 데이터 구조를 제공해야 한다.

즉 price history API는 "단순 조회"가 아니라 "차트 데이터 공급 API"로 이해하는 것이 맞다.

---

## 16. 포트폴리오에서 강조할 핵심 설계 포인트

### 17-1. 외부 API 호출 비용 통제 구조

가장 강하게 강조해야 할 부분 중 하나다.

- 내부 검색과 외부 검색 분리
- 배치에서 1차 `(몰 + 브랜드)` 검색 후 2차 개별 검색
- Redis quota key 기반 일일 호출량 제어

이 세 가지는 모두 "외부 API를 단순 연동한 것이 아니라, 운영 비용을 고려해 설계했다"는 포인트를 만든다.

### 17-2. MySQL - ES read model 분리

포트폴리오에서 기술적으로 가장 아키텍처다운 포인트다.

- MySQL source of truth
- ES read model
- async task + retry

단순 검색 엔진 도입보다 훨씬 설계 포인트가 살아난다.

### 17-3. 가격 배치와 스냅샷 데이터 모델링

- products 최신 상태
- price_history 일자별 상태

이 둘을 분리한 것은 "현재 상태"와 "시간 축 상태"를 분리한 전형적 백엔드 설계 포인트다.

### 17-4. 자동 그룹핑 포기와 사용자 정의 비교 그룹 전환

이건 아주 좋은 설계 서사다.

- 처음에는 검색 결과 자동 그룹핑 시도
- 오탐이 많아짐
- 그룹핑은 시스템 책임보다 사용자 책임으로 전환
- 검색은 개별 상품, 비교는 워치리스트 그룹, 탐색은 추천으로 재구성

즉 실패한 시도를 버린 것이 아니라, 문제 정의를 재구성한 사례다.

### 17-5. 추천 기능의 단계적 확장 가능성

현재 구현:

- 같은 브랜드 추천
- 브랜드 무관 유사 추천

향후 확장:

- Redis co-view 추천
- 임베딩 기반 추천

즉 "지금은 규칙 기반 추천으로 시작하고, 이후 행동 데이터 기반 추천으로 확장 가능한 구조"라는 식으로 설명 가능하다.

---

## 17. 트러블슈팅으로 바로 옮길 수 있는 소재 정리

### 소재 1. 외부 쇼핑 API 호출량 과다 문제

핵심 메시지:

- 검색/배치 모두 외부 API에 의존하면 quota와 비용 문제가 빠르게 드러남
- 내부 검색/외부 검색 분리
- 1차 브랜드 단위, 2차 개별 검색으로 재설계

### 소재 2. MySQL - ES 비동기 동기화 정합성 문제

핵심 메시지:

- 검색 인덱스는 파생 데이터
- ES 실패가 원본 정합성을 침해하면 안 됨
- task 기반 eventually consistent 구조와 retry 적용

### 소재 3. 배치 실행 중 `price_history` 저장 실패 문제

핵심 메시지:

- price null insert로 step fail
- 작은 저장 누락이 배치 전체를 멈춤
- 데이터 생성 책임을 명시적으로 보완

### 소재 4. 자동 그룹핑 오탐 문제

핵심 메시지:

- 패션 상품명은 토큰 유사도만으로 동일 상품 판별이 어려움
- 잘못 묶는 오탐이 더 치명적
- 검색 그룹핑 포기 후 추천 + 사용자 정의 비교 그룹으로 구조 재설계

## 18. 아직 남은 개선 과제

### 높은 우선순위

1. 카테고리 저장/조회 시 패션 whitelist 적용
2. 프론트의 refresh token 자동 재발급 연결 확인
3. ES total hit count 정확 반영
4. 메인/마이페이지 프론트 연결

### 중간 우선순위

1. Redis 기반 co-view 추천 구현
2. 배치 chunk 전환
3. 카테고리 내부 분류 체계 정제

### 낮은 우선순위

1. 추천 고도화(embedding, click history)
2. ES 인덱스 blue-green 식 재생성 전략
3. 관리자용 배치/색인 운영 화면

---

## 19. 지금 시점의 API 지도

### 인증

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### 검색 / 저장

- `GET /products/search`
- `GET /products/search/external`
- `POST /products/select`
- `POST /products/search-index/rebuild`

### 상품 / 가격 / 추천

- `GET /products/{id}`
- `GET /products/{id}/price-history?days=30`
- `GET /products/{id}/recommendations?limit=10`

### 메인 / 브라우징

- `GET /products/latest?page=0&size=20`
- `GET /products/categories/{categoryId}?page=0&size=20`
- `GET /categories`
- `GET /categories/{parentId}/children`

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

## 20. 다음 세션에서 바로 이어서 볼 포인트

1. 카테고리 체계를 패션 전용으로 어떻게 정제할지
2. refresh token 자동 재발급이 프론트에서 실제로 어떻게 붙는지
3. 메인페이지 browse API와 카테고리 트리 연결 방식
4. 워치리스트 그룹 UI/UX를 어떻게 보여줄지
5. co-view 추천을 Redis ZSET으로 확장할지
6. 배치를 chunk 기반으로 전환할지

---

## 21. 마지막 검증 상태

마지막 시점 기준으로 `./gradlew.bat test`는 통과한 상태다.

남아 있는 구현적 메모:

- `ProductSearchService`의 `Specification.where(...)` deprecated warning 1건 존재
- 기능에는 영향 없음

---

## 부록: 포트폴리오 문장으로 압축할 때의 핵심 문구

### 프로젝트 요약용

- 외부 쇼핑 API 호출 비용을 통제하기 위해 내부 검색과 외부 검색을 분리하고, 브랜드/몰 기반 2단계 가격 배치 구조를 설계한 가격 추적 서비스
- MySQL을 source of truth로 두고 Elasticsearch를 검색 전용 read model로 분리하여 async task 기반 동기화 구조를 구현한 프로젝트
- 상품 현재 상태와 일자별 가격 스냅샷을 분리해 `products`와 `price_history`를 설계하고, 목표가/최저가/하락률 알림까지 연결한 백엔드 시스템

### 트러블슈팅 요약용

- 외부 API 호출량 과다 문제를 검색/배치 분리 구조로 해결
- MySQL - ES 정합성 문제를 task 기반 비동기 동기화와 retry 구조로 해결
- 가격 히스토리 저장 누락으로 인한 배치 실패 문제를 저장 책임 명확화로 해결
- 자동 그룹핑 오탐 문제를 검색 단순화 + 추천 + 사용자 정의 비교 그룹 구조로 재설계

---

## 부록: 포트폴리오 슬라이드별 최종 문구

아래 문구는 현재 `IgLoo` 포트폴리오의 페이지별 어조를 맞춘 버전이다.

- 프로젝트 개요 페이지: 존댓말 종결
- 아키텍처 / 플로우 페이지: 명사형 / 구조 설명형 종결
- 트러블 슈팅 페이지: `Problem / Cause / Solution / Result` 짧은 문장 종결

권장 구성은 아래와 같다.

1. 프로젝트 개요
2. 시스템 아키텍처
3. 핵심 플로우
4. 트러블 슈팅 1
5. 트러블 슈팅 2
6. 트러블 슈팅 3
7. 트러블 슈팅 4

### 슬라이드 1. 프로젝트 개요

#### 제목

`PRICEWATCH - 상품 가격 추적 서비스`

#### 프로젝트 개요

`PRICEWATCH는 사용자가 관심 상품을 저장하면 가격 변동을 추적하고, 목표가 도달 및 최저가 갱신 시 알림을 제공하는 상품 가격 추적 서비스입니다.`

`내부 검색과 외부 쇼핑 API 연동을 분리하여 호출 비용을 통제하는 구조로 설계하였습니다.`

`또한 Spring Batch 기반 가격 갱신, 일자별 price_history 저장, Elasticsearch 기반 내부 검색 구조를 적용한 프로젝트입니다.`

#### 기간 / 기여도

`기간 : [작성 필요]`

`기여도 : [작성 필요]`

#### 담당 구현 기능

- `JWT Access / Refresh Token 기반 인증 구조 구현`
- `내부 검색 / 외부 검색 분리 및 네이버 쇼핑 API 연동`
- `상품 선택 저장, 이미지 저장, 카테고리 트리 저장 기능 구현`
- `Elasticsearch 기반 내부 검색 및 비동기 색인 동기화 구조 구현`
- `Spring Batch 기반 가격 갱신 및 price_history 스냅샷 저장`
- `목표가 / 최저가 / 가격 하락 알림 및 이메일 Outbox 구현`
- `워치리스트 비교 그룹 및 추천 기능 구현`

#### 기술 스택

BACKEND

- `Java 21 / Spring Boot 3.5`
- `Spring Data JPA`
- `Spring Security / JWT`
- `Spring Batch`
- `Elasticsearch`
- `Redis / Redisson`
- `MySQL`

INFRA

- `Docker / Docker Compose`
- `AWS EC2`
- `Docker Hub`

---

### 슬라이드 2. 시스템 아키텍처

#### 제목

`시스템 아키텍처`

#### 다이어그램 구성 예시

`Client`

`Web / App`

↓

`Spring Boot API Server`

→ `MySQL`

`상품 / 가격 / 워치리스트 / 알림 / Task 원본 데이터 관리`

→ `Elasticsearch`

`내부 검색 전용 read model`

→ `Redis`

`외부 검색 캐시 / quota / 분산 락`

→ `Naver Shopping API`

`외부 상품 검색 및 가격 정보 수집`

→ `Email`

`Outbox 기반 비동기 메일 발송`

#### 다이어그램 하단 설명 문구

`MySQL을 source of truth로 두고, Elasticsearch는 검색 전용 read model로 분리한 구조.`

`Redis는 외부 검색 캐시, quota 제어, 락에 활용하는 구조.`

`외부 상품 수집과 가격 갱신은 Naver Shopping API 연동 구조.`

#### 본문

`Client - Spring Boot - MySQL / Redis / Elasticsearch / Naver Shopping API 연동 구조.`

`MySQL을 source of truth로 두고, Elasticsearch는 검색 전용 read model로 분리한 구조.`

`가격 배치와 외부 검색은 Naver Shopping API를 통해 상품 가격 및 검색 결과 수집 구조.`

`Redis는 quota 제어, 캐시, 락에 활용하고, 알림 / 워치리스트 / 비교 그룹은 MySQL 기준으로 관리하는 구조.`

#### 주요 특징

- `Client -> Spring Boot API 서버`
- `MySQL 기반 원본 데이터 관리`
- `Elasticsearch 기반 내부 검색`
- `Redis 기반 quota / cache / lock 제어`
- `Naver Shopping API 기반 외부 상품 수집`
- `Email Outbox 기반 비동기 발송 구조`

#### 설계 목적

- `원본 데이터와 검색 인덱스 역할 분리 목적`
- `검색 성능 확보 목적`
- `외부 API 의존 비용 통제 목적`
- `운영 확장성 확보 목적`

---

### 슬라이드 3. 핵심 플로우

#### 제목

`핵심 플로우`

#### 다이어그램 구성 예시 1. 상품 검색 및 저장 플로우

`사용자 검색 요청`

↓

`GET /products/search`

↓

`Elasticsearch 검색`

↓ 실패 시

`DB fallback 검색`

↓

`사용자 외부 검색 요청`

↓

`GET /products/search/external`

↓

`Redis 캐시 확인`

↓ miss

`Naver Shopping API 호출`

↓

`상품 선택 저장`

↓

`products / category / image / price_history 저장`

↓

`Task 생성`

↓

`비동기 ES upsert`

#### 다이어그램 구성 예시 2. 가격 배치 플로우

`Batch Trigger`

↓

`1차 (몰 + 브랜드) 검색`

↓

`Naver Shopping API 호출`

↓

`naverProductId / externalKey 매칭`

↓ 미매칭만

`2차 개별 상품 검색`

↓

`products 최신 가격 갱신`

↓

`price_history 저장`

↓

`notification 생성`

↓

`ES sync task 발행`

#### 다이어그램 하단 설명 문구

`내부 검색은 Elasticsearch 우선, 실패 시 DB fallback 구조.`

`외부 검색은 Redis 캐시 및 single-flight lock을 거쳐 Naver Shopping API를 호출하는 구조.`

`가격 배치는 1차 브랜드 단위, 2차 개별 검색 구조로 외부 API 호출량을 절감하는 구조.`

#### 본문

`1. 상품 검색 플로우`

`내부 검색은 Elasticsearch 우선, 실패 시 DB fallback 구조.`

`외부 검색은 네이버 쇼핑 API 별도 진입점 구조.`

`2. 상품 저장 플로우`

`사용자 선택 상품 저장 시 products / category / image / price_history 동시 반영 구조.`

`저장 후 MySQL -> ES 비동기 sync task 발행 구조.`

`3. 가격 배치 플로우`

`1차는 (몰 + 브랜드) 단위 검색, 2차는 미매칭 상품 개별 검색 구조.`

`갱신 성공 시 products 최신 가격, price_history, notification, ES sync까지 연결 구조.`

#### 주요 포인트

- `내부 검색 / 외부 검색 분리`
- `상품 선택 저장 시 당일 snapshot 보장`
- `MySQL -> ES async sync`
- `1차 브랜드 단위 / 2차 개별 단위 가격 배치`
- `알림 / 추천 / 비교 그룹으로 이어지는 사용자 흐름`

---

### 슬라이드 4. 트러블 슈팅 - 외부 쇼핑 API 호출량 제약 대응

#### 제목

`트러블 슈팅`

`외부 쇼핑 API 호출량 제약 대응을 위한 가격 배치 구조 개선`

#### Problem

`상품 단위 개별 검색 방식으로 가격 배치를 수행할 경우, 일일 호출 한도 내에서 대량 상품 가격 추적이 어려운 문제 발생.`

`저장 상품 수가 증가할수록 배치 처리 가능 상품 수가 API 호출 한도에 직접 제한되는 구조 확인.`

#### Cause

`기존 방식은 상품마다 외부 API를 개별 호출하는 구조.`

`호출 수가 상품 수와 정비례하여 데이터 규모가 커질수록 배치 효율이 급격히 저하되는 구조.`

#### Solution

`배치 구조를 상품 단위 검색이 아닌 (몰 + 브랜드) 단위 1차 검색 구조로 재설계.`

`1차 결과에서 매칭되지 않은 상품만 2차 개별 검색으로 처리하는 2단계 배치 구조 적용.`

`외부 검색 경로에는 Redis quota 키, 검색 캐시, single-flight lock 구조 적용.`

#### Result

`동일 호출량 기준 더 많은 상품을 갱신할 수 있는 배치 구조 확보.`

`상품 수 증가 상황에서도 호출량을 통제할 수 있는 운영 구조 확보.`

`외부 API 사용량을 기능이 아닌 구조 차원에서 절감하는 기반 마련.`

---

### 슬라이드 5. 트러블 슈팅 - DB 기반 검색 한계와 Elasticsearch 도입

#### 제목

`트러블 슈팅`

`DB 기반 검색 한계와 Elasticsearch 도입`

#### Problem

`기존 내부 검색은 문자열 포함 여부 중심으로 동작하여 한글 / 영문 혼합 상품명, 동의어, 관련도 정렬 처리에 한계 존재.`

`색상 등 일부 토큰만 일치하는 결과가 검색에 노출되어 검색 품질 저하 문제 발생.`

#### Cause

`RDB 기반 contains 검색은 형태 분석, 동의어 처리, relevance scoring에 적합하지 않은 구조.`

`브랜드 / 제목 / 카테고리 / 색상 토큰의 중요도를 세밀하게 반영하기 어려운 상태.`

#### Solution

`Elasticsearch 기반 검색 인덱스 구조 적용.`

`동의어 사전, relevance scoring, phrase matching, 검색 결과 후처리 규칙을 적용하기 위해 Elasticsearch 기반 내부 검색 구조 도입.`

`브랜드 / 제목 / 카테고리 기준 relevance scoring 구조 적용.`

#### Result

`한글 / 영문 혼합 검색과 관련도 정렬 품질 개선.`

`DB 조회 중심 구조에서 검색 엔진 기반 구조로 분리 완료.`

`향후 상품 수 증가에도 대응 가능한 검색 확장 기반 확보.`

---

### 슬라이드 6. 트러블 슈팅 - MySQL / Elasticsearch 정합성 문제

#### 제목

`트러블 슈팅`

`MySQL - Elasticsearch 비동기 동기화 정합성 문제`

#### Problem

`상품 저장 직후 검색 인덱스 반영이 늦어질 경우, 다른 사용자가 같은 상품을 다시 외부 검색 후 중복 저장할 수 있는 문제 존재.`

`네이버 API 사용량을 아껴야 하는 구조에서 검색 인덱스 반영 지연은 곧 중복 적재와 불필요한 외부 호출로 이어질 수 있는 상태.`

#### Cause

`Logstash 기반 동기화는 주기적 수집 중심 구조로 동작하여 반영 시점이 상대적으로 느린 방식.`

`이 프로젝트는 배치 수집보다 사용자 저장 이벤트 직후 검색 가능 상태가 더 중요한 구조.`

#### Solution

`MySQL을 source of truth로 두고 Elasticsearch는 read model로 분리한 구조 적용.`

`Logstash 대신 Task 기반 이벤트 발행 + 비동기 리스너 기반 실시간 동기화 구조 적용.`

`실패 Task는 DB에 남기고 재시도 스케줄러로 eventually consistent 구조 보완.`

#### Result

`상품 저장 직후 검색 인덱스 반영 지연 최소화.`

`중복 상품 적재 가능성과 불필요한 외부 API 재호출 가능성 완화.`

`원본 정합성과 검색 반영 속도를 함께 고려한 구조 확보.`

---

### 슬라이드 7. 트러블 슈팅 - 자동 그룹핑 오탐 문제와 구조 재설계

#### 제목

`트러블 슈팅`

`검색 결과 자동 그룹핑 오탐 문제와 구조 재설계`

#### Problem

`네이버 API 기준 동일 상품이더라도 판매처별로 서로 다른 이름으로 등록된 경우가 많아 검색 결과만으로 즉시 자동 그룹핑하기 어려운 문제 발생.`

`패션 상품명은 색상, 슬리브, 핏, 워싱, 모델코드 차이로 실제 상품이 갈리는 경우가 많아 토큰 유사도만으로 정확한 그룹핑이 어려운 상태.`

#### Cause

`검색 결과 그룹핑은 시스템이 동일 상품 여부를 자동 판별해야 하는 구조.`

`판매처별 제목 표기 방식 차이까지 고려하면, 잘못 묶는 오탐이 안 묶이는 경우보다 더 치명적인 문제.`

#### Solution

`검색 결과 자동 그룹핑 제거 후 검색은 개별 상품 중심으로 단순화.`

`비교는 워치리스트 비교 그룹으로 이동하여 사용자가 원하는 상품을 직접 그룹으로 구성하는 구조로 전환.`

`탐색 흐름 보완을 위해 브랜드 및 상품명 토큰 점수 기반 동일 브랜드 추천과 유사 상품 추천 기능 추가.`

#### Result

`검색 정확도를 해치던 자동 그룹핑 오탐 문제 제거.`

`비교 정확도는 사용자가 직접 그룹을 구성하는 방식으로 보완.`

`검색, 비교, 추천의 책임을 분리한 구조 완성.`

---

### 슬라이드 8. 트러블 슈팅 - 외부 플랫폼 카테고리 적재로 인한 도메인 오염 문제

#### 제목

`트러블 슈팅`

`외부 플랫폼 카테고리 적재로 인한 도메인 카테고리 오염 문제`

#### Problem

`패션 상품 중심 서비스로 설계했지만, 카테고리 트리에 패션 외 분류가 함께 누적되어 프론트 카테고리 탐색에 혼선을 주는 문제 발생.`

`브랜드 검색 결과를 대량 적재하는 과정에서 서비스 의도와 무관한 외부 플랫폼 분류까지 그대로 들어오는 문제 발생.`

#### Cause

`현재 카테고리 구조는 네이버 쇼핑 API의 category1~4를 그대로 누적 저장하는 방식.`

`외부 플랫폼의 분류 체계를 내부 서비스용 탐색 체계로 바로 사용하는 구조적 한계 존재.`

#### Solution

`카테고리를 외부 원본 데이터를 그대로 적재하는 대상이 아니라, 서비스 탐색을 위한 내부 분류 체계로 바라보도록 기준 재정의.`

`이후 패션 계열 whitelist 기반 저장 / 조회 필터를 적용할 수 있도록 개선 방향 정리.`

`즉시 기능 구현보다 도메인 경계 정의를 먼저 명확히 하는 방향 선택.`

#### Result

`외부 플랫폼 데이터와 서비스 내부 탐색 체계를 분리해서 바라보는 기준 확보.`

`카테고리 기능을 단순 조회 기능이 아니라 도메인 모델링 과제로 정리 가능 상태 확보.`

`추후 패션 전용 카테고리 체계로 정제할 수 있는 설계 방향 확보.`
