# DESKIT

데스크테리어 전문 라이브 커머스 플랫폼 DESKIT 입니다. 
상품 구매, 라이브 방송 송출 및 시청, 실시간 채팅, 챗봇 문의 기능을 포함합니다.

## 주요 기능
- 상품 및 셋업 탐색, 장바구니, 주문/결제(토스 연동).
- 라이브 방송 + 채팅(WebSocket + OpenVidu), VOD 재생.
- 셀러 상품/라이브 관리, 관리자 대시보드.
- 정책 문서 RAG 기반 챗봇과 선호도 추천.
- 소셜 로그인(네이버/구글/카카오), 이메일 알림.

## 기술 스택
- 백엔드: Spring Boot 3.4, Java 17, JPA/JDBC, Spring Security, WebSocket, Redis.
- AI: Spring AI + OpenAI 모델, Redis 벡터 스토어.
- 인프라: MySQL, Redis, OpenVidu, Naver Cloud Platform(NCP).
- 프론트엔드: Vue 3, Vite, TypeScript.

## 프로젝트 구조
- `src/main/java`: Spring Boot 애플리케이션 소스.
- `src/main/resources`: 설정, SQL 시드, RAG 시드 문서.
- `front`: Vue 클라이언트 앱.
- `docs`: 정책 및 프로젝트 문서.

## 로컬 실행
사전 준비:
- JDK 17
- Node.js 18+
- MySQL 8
- Redis
- (선택) OpenVidu, OpenAI, SendGrid, Solapi, Toss, S3 호환 스토리지

### 1) 백엔드 설정
로컬 프로파일을 만들고 시크릿 값을 본인 것으로 교체:
- `src/main/resources/application-local.properties`

교체 권장 항목:
- `spring.datasource.*`
- `spring.data.redis.*`
- `spring.ai.openai.*`
- `openvidu.*`
- `cloud.aws.*`
- `spring.sendgrid.api-key`
- `solapi.*`
- `toss.payments.secret-key`
- `spring.security.oauth2.client.registration.*`

실행 시 프로파일 적용:
```
SPRING_PROFILES_ACTIVE=local
```

### 2) DB 준비
SQL 스크립트 경로:
- `src/main/resources/sql`

### 3) 백엔드 실행
```
./gradlew bootRun
```
Windows PowerShell:
```
.\gradlew.bat bootRun
```

### 4) 프론트엔드 실행
```
cd front
npm install
npm run dev
```

빌드/미리보기:
```
npm run build
npm run preview
```

## 참고
- 접속 주소: `https://ssg.deskit.o-r.kr` (NCP 서버 -> 로컬 마이그레이션, 26.01.23)
- AWS로 서버 이전 예정입니다.
- RAG 시드 파일은 `src/main/resources/rag/seed`에 저장, 파일 추가 시 자동 Ingest됩니다.
